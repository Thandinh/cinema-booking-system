package com.cinema.booking.service;

import com.cinema.booking.dto.request.CreateBookingRequest;
import com.cinema.booking.dto.request.HoldSeatRequest;
import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.dto.response.HoldSeatResponse;
import com.cinema.booking.dto.response.TicketResponse;
import com.cinema.booking.entity.Cinema;
import com.cinema.booking.entity.Movie;
import com.cinema.booking.entity.Room;
import com.cinema.booking.entity.Seat;
import com.cinema.booking.entity.SeatStatus;
import com.cinema.booking.entity.Showtime;
import com.cinema.booking.entity.User;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.enums.MovieStatus;
import com.cinema.booking.enums.SeatStatusType;
import com.cinema.booking.enums.SeatType;
import com.cinema.booking.enums.ShowtimeStatus;
import com.cinema.booking.enums.TicketStatus;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.CinemaRepository;
import com.cinema.booking.repository.MovieRepository;
import com.cinema.booking.repository.PaymentRepository;
import com.cinema.booking.repository.RoomRepository;
import com.cinema.booking.repository.SeatRepository;
import com.cinema.booking.repository.SeatStatusRepository;
import com.cinema.booking.repository.ShowtimeRepository;
import com.cinema.booking.repository.TicketRepository;
import com.cinema.booking.repository.UserRepository;
import com.cinema.booking.support.PostgresIntegrationTest;
import com.cinema.booking.websocket.SeatStatusPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "booking.seat-hold-minutes=5",
        "booking.pending-timeout-minutes=5",
        "showtime.booking-cutoff-minutes=15",
        "ticket.check-in-early-minutes=180",
        "ticket.check-in-late-minutes=30",
        "ticket.qr-secret=test-ticket-qr-secret-32-characters-minimum"
})
class BookingWorkflowIntegrationTest extends PostgresIntegrationTest {

    private static final BigDecimal BASE_PRICE = new BigDecimal("100000.00");

    @Autowired
    BookingService bookingService;

    @Autowired
    TicketQrCodeService ticketQrCodeService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    MovieRepository movieRepository;

    @Autowired
    CinemaRepository cinemaRepository;

    @Autowired
    RoomRepository roomRepository;

    @Autowired
    SeatRepository seatRepository;

    @Autowired
    ShowtimeRepository showtimeRepository;

    @Autowired
    SeatStatusRepository seatStatusRepository;

    @Autowired
    BookingRepository bookingRepository;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    TicketRepository ticketRepository;

    @MockitoBean
    EmailService emailService;

    @MockitoBean
    SeatStatusPublisher seatStatusPublisher;

    User customer;
    User staff;

    @BeforeEach
    void setUp() {
        clearBusinessData();
        customer = getOrCreateUser("user1", "user1@cinema.com");
        staff = getOrCreateUser("staff1", "staff@cinema.com");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        clearBusinessData();
    }

    @Test
    void holdSeats_shouldMarkSeatsHoldAndRejectSecondHold() {
        TestShowtimeData data = createShowtimeData();
        authenticateAs(customer, "BOOKING_CREATE");

        HoldSeatResponse response = bookingService.holdSeats(new HoldSeatRequest(
                data.showtime().getId(),
                List.of(data.seats().getFirst().getId())
        ));

        assertThat(response.getShowtimeId()).isEqualTo(data.showtime().getId());
        assertThat(response.getHeldSeatIds()).containsExactly(data.seats().getFirst().getId());
        assertThat(response.getHoldUntil()).isAfter(LocalDateTime.now());
        assertThat(response.getEstimatedTotalPrice()).isEqualByComparingTo(BASE_PRICE);

        SeatStatus heldSeat = seatStatusFor(data.showtime(), data.seats().getFirst());
        assertThat(heldSeat.getStatus()).isEqualTo(SeatStatusType.HOLD);
        assertThat(heldSeat.getHoldBy().getId()).isEqualTo(customer.getId());
        assertThat(heldSeat.getHoldUntil()).isAfter(LocalDateTime.now());

        assertThatThrownBy(() -> bookingService.holdSeats(new HoldSeatRequest(
                data.showtime().getId(),
                List.of(data.seats().getFirst().getId())
        )))
                .isInstanceOfSatisfying(AppException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SEAT_NOT_AVAILABLE));
    }

    @Test
    void paymentSuccess_shouldBookSeatsAndCreateSignedTickets() {
        TestShowtimeData data = createShowtimeData();
        authenticateAs(customer, "BOOKING_CREATE");
        List<UUID> selectedSeatIds = data.seats().stream().limit(2).map(Seat::getId).toList();

        bookingService.holdSeats(new HoldSeatRequest(data.showtime().getId(), selectedSeatIds));
        BookingResponse pendingBooking = bookingService.createBooking(CreateBookingRequest.builder()
                .showtimeId(data.showtime().getId())
                .seatIds(selectedSeatIds)
                .build());

        assertThat(pendingBooking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(pendingBooking.getPaymentExpiresAt()).isAfter(LocalDateTime.now());

        BookingResponse paidBooking = bookingService.handlePaymentSuccess(pendingBooking.getSecureToken());

        assertThat(paidBooking.getStatus()).isEqualTo(BookingStatus.SUCCESS);
        assertThat(bookingRepository.findById(paidBooking.getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.SUCCESS);
        assertThat(seatStatusRepository.findAllByShowtimeId(data.showtime().getId()))
                .filteredOn(seatStatus -> selectedSeatIds.contains(seatStatus.getSeat().getId()))
                .allSatisfy(seatStatus -> {
                    assertThat(seatStatus.getStatus()).isEqualTo(SeatStatusType.BOOKED);
                    assertThat(seatStatus.getHoldBy()).isNull();
                    assertThat(seatStatus.getHoldUntil()).isNull();
                });
        assertThat(ticketRepository.findAll())
                .hasSize(2)
                .allSatisfy(ticket -> {
                    assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ACTIVE);
                    assertThat(ticketQrCodeService.isValidSignedToken(ticket.getQrCode())).isTrue();
                });
    }

    @Test
    void paymentFailure_shouldMarkBookingFailedAndReleaseHeldSeats() {
        TestShowtimeData data = createShowtimeData();
        authenticateAs(customer, "BOOKING_CREATE");
        List<UUID> selectedSeatIds = data.seats().stream().limit(2).map(Seat::getId).toList();

        bookingService.holdSeats(new HoldSeatRequest(data.showtime().getId(), selectedSeatIds));
        BookingResponse pendingBooking = bookingService.createBooking(CreateBookingRequest.builder()
                .showtimeId(data.showtime().getId())
                .seatIds(selectedSeatIds)
                .build());

        BookingResponse failedBooking = bookingService.handlePaymentFailure(pendingBooking.getSecureToken());

        assertThat(failedBooking.getStatus()).isEqualTo(BookingStatus.FAILED);
        assertThat(bookingRepository.findById(failedBooking.getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.FAILED);
        assertThat(seatStatusRepository.findAllByShowtimeId(data.showtime().getId()))
                .filteredOn(seatStatus -> selectedSeatIds.contains(seatStatus.getSeat().getId()))
                .allSatisfy(seatStatus -> {
                    assertThat(seatStatus.getStatus()).isEqualTo(SeatStatusType.AVAILABLE);
                    assertThat(seatStatus.getHoldBy()).isNull();
                    assertThat(seatStatus.getHoldUntil()).isNull();
                });
        assertThat(ticketRepository.findAll()).isEmpty();
    }

    @Test
    void checkInTicket_shouldRequireCorrectCinemaAndShowtimeBeforeMarkingUsed() {
        TestShowtimeData data = createShowtimeData();
        authenticateAs(customer, "BOOKING_CREATE");
        List<UUID> selectedSeatIds = List.of(data.seats().getFirst().getId());

        bookingService.holdSeats(new HoldSeatRequest(data.showtime().getId(), selectedSeatIds));
        BookingResponse pendingBooking = bookingService.createBooking(CreateBookingRequest.builder()
                .showtimeId(data.showtime().getId())
                .seatIds(selectedSeatIds)
                .build());
        bookingService.handlePaymentSuccess(pendingBooking.getSecureToken());
        String qrCode = ticketRepository.findAll().getFirst().getQrCode();

        TestShowtimeData otherData = createShowtimeData();
        authenticateAs(staff, "TICKET_CHECKIN");

        assertThatThrownBy(() -> bookingService.checkInTicket(
                qrCode,
                otherData.cinema().getId(),
                data.showtime().getId()
        ))
                .isInstanceOfSatisfying(AppException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TICKET_WRONG_CINEMA));

        assertThatThrownBy(() -> bookingService.checkInTicket(
                qrCode,
                data.cinema().getId(),
                otherData.showtime().getId()
        ))
                .isInstanceOfSatisfying(AppException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TICKET_WRONG_SHOWTIME));

        TicketResponse firstScan = bookingService.checkInTicket(
                qrCode,
                data.cinema().getId(),
                data.showtime().getId()
        );

        assertThat(firstScan.getStatus()).isEqualTo(TicketStatus.USED);
        assertThat(firstScan.getAlreadyCheckedIn()).isFalse();
        assertThat(firstScan.getCheckedInById()).isEqualTo(staff.getId());

        TicketResponse secondScan = bookingService.checkInTicket(
                qrCode,
                data.cinema().getId(),
                data.showtime().getId()
        );

        assertThat(secondScan.getStatus()).isEqualTo(TicketStatus.USED);
        assertThat(secondScan.getAlreadyCheckedIn()).isTrue();
        assertThat(secondScan.getCheckedInById()).isEqualTo(staff.getId());
    }

    private void clearBusinessData() {
        ticketRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        bookingRepository.deleteAllInBatch();
        seatStatusRepository.deleteAllInBatch();
        showtimeRepository.deleteAllInBatch();
        seatRepository.deleteAllInBatch();
        roomRepository.deleteAllInBatch();
        cinemaRepository.deleteAllInBatch();
        movieRepository.deleteAllInBatch();
    }

    private User getOrCreateUser(String username, String email) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(User.builder()
                        .username(username)
                        .password("{noop}123456")
                        .firstName("Test")
                        .lastName("User")
                        .email(email)
                        .emailVerified(true)
                        .isActive(true)
                        .isDeleted(false)
                        .build()));
    }

    private TestShowtimeData createShowtimeData() {
        String suffix = UUID.randomUUID().toString();
        Movie movie = movieRepository.save(Movie.builder()
                .title("Integration Movie " + suffix)
                .description("Movie for booking workflow integration test")
                .duration(120)
                .genre("Action")
                .releaseDate(LocalDate.now().minusDays(1))
                .status(MovieStatus.NOW_SHOWING)
                .isDeleted(false)
                .build());
        Cinema cinema = cinemaRepository.save(Cinema.builder()
                .name("Integration Cinema " + suffix)
                .address("123 Test Street")
                .city("TP Ho Chi Minh")
                .latitude(10.7769)
                .longitude(106.7009)
                .isActive(true)
                .isDeleted(false)
                .build());
        Room room = roomRepository.save(Room.builder()
                .cinema(cinema)
                .name("Screen 01")
                .isDeleted(false)
                .build());
        List<Seat> seats = seatRepository.saveAll(List.of(
                seat(room, "A", 1, 0),
                seat(room, "A", 2, 1),
                seat(room, "A", 3, 2)
        ));
        Showtime showtime = showtimeRepository.save(Showtime.builder()
                .movie(movie)
                .room(room)
                .startTime(LocalDateTime.now().plusHours(2))
                .endTime(LocalDateTime.now().plusHours(4))
                .basePrice(BASE_PRICE)
                .status(ShowtimeStatus.UPCOMING)
                .isDeleted(false)
                .build());
        seatStatusRepository.saveAll(seats.stream()
                .map(seat -> SeatStatus.builder()
                        .showtime(showtime)
                        .seat(seat)
                        .status(SeatStatusType.AVAILABLE)
                        .version(0)
                        .build())
                .toList());
        return new TestShowtimeData(movie, cinema, room, seats, showtime);
    }

    private Seat seat(Room room, String rowLabel, int seatNumber, int colIndex) {
        return Seat.builder()
                .room(room)
                .rowLabel(rowLabel)
                .seatNumber(seatNumber)
                .rowIndex(0)
                .colIndex(colIndex)
                .seatType(SeatType.NORMAL)
                .priceMultiplier(BigDecimal.ONE)
                .isDeleted(false)
                .build();
    }

    private SeatStatus seatStatusFor(Showtime showtime, Seat seat) {
        return seatStatusRepository.findAllByShowtimeId(showtime.getId()).stream()
                .filter(seatStatus -> seatStatus.getSeat().getId().equals(seat.getId()))
                .findFirst()
                .orElseThrow();
    }

    private void authenticateAs(User user, String... authorities) {
        Jwt jwt = Jwt.withTokenValue("test-token-" + user.getId())
                .header("alg", "none")
                .subject(user.getUsername())
                .claim("userId", user.getId().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        List<SimpleGrantedAuthority> grantedAuthorities = List.of(authorities).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, grantedAuthorities, user.getUsername())
        );
    }

    private record TestShowtimeData(
            Movie movie,
            Cinema cinema,
            Room room,
            List<Seat> seats,
            Showtime showtime
    ) {
    }
}
