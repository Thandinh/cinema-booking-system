package com.cinema.booking.service;

import com.cinema.booking.dto.request.CreateBookingRequest;
import com.cinema.booking.dto.request.HoldSeatRequest;
import com.cinema.booking.dto.request.RefundCompleteRequest;
import com.cinema.booking.dto.request.ShowtimeCancelRequest;
import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.dto.response.HoldSeatResponse;
import com.cinema.booking.dto.response.RefundResponse;
import com.cinema.booking.dto.response.TicketResponse;
import com.cinema.booking.entity.Cinema;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.Payment;
import com.cinema.booking.entity.Movie;
import com.cinema.booking.entity.Promotion;
import com.cinema.booking.entity.Room;
import com.cinema.booking.entity.Seat;
import com.cinema.booking.entity.SeatStatus;
import com.cinema.booking.entity.StaffCinema;
import com.cinema.booking.entity.StaffCinemaId;
import com.cinema.booking.entity.Showtime;
import com.cinema.booking.entity.User;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.enums.DiscountType;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.enums.MovieStatus;
import com.cinema.booking.enums.PaymentMethod;
import com.cinema.booking.enums.PaymentEventType;
import com.cinema.booking.enums.PaymentStatus;
import com.cinema.booking.enums.RefundStatus;
import com.cinema.booking.enums.SeatStatusType;
import com.cinema.booking.enums.SeatType;
import com.cinema.booking.enums.ShowtimeStatus;
import com.cinema.booking.enums.TicketStatus;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.CinemaRepository;
import com.cinema.booking.repository.MovieRepository;
import com.cinema.booking.repository.PaymentRepository;
import com.cinema.booking.repository.PaymentEventRepository;
import com.cinema.booking.repository.PromotionRepository;
import com.cinema.booking.repository.RefundRepository;
import com.cinema.booking.repository.RoomRepository;
import com.cinema.booking.repository.SeatRepository;
import com.cinema.booking.repository.SeatStatusRepository;
import com.cinema.booking.repository.ShowtimeRepository;
import com.cinema.booking.repository.StaffCinemaRepository;
import com.cinema.booking.repository.TicketRepository;
import com.cinema.booking.repository.UserRepository;
import com.cinema.booking.support.PostgresIntegrationTest;
import com.cinema.booking.websocket.SeatStatusPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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
    PaymentService paymentService;

    @Autowired
    ShowtimeService showtimeService;

    @Autowired
    ShowtimeStatusSyncService showtimeStatusSyncService;

    @Autowired
    RefundService refundService;

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
    PaymentEventRepository paymentEventRepository;

    @Autowired
    RefundRepository refundRepository;

    @Autowired
    PromotionRepository promotionRepository;

    @Autowired
    TicketRepository ticketRepository;

    @Autowired
    StaffCinemaRepository staffCinemaRepository;

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
    void createBooking_shouldReuseTheActiveBookingForAnIdempotentRetry() {
        TestShowtimeData data = createShowtimeData();
        authenticateAs(customer, "BOOKING_CREATE");
        List<UUID> selectedSeatIds = data.seats().stream().limit(2).map(Seat::getId).toList();

        bookingService.holdSeats(new HoldSeatRequest(data.showtime().getId(), selectedSeatIds));
        BookingResponse first = bookingService.createBooking(CreateBookingRequest.builder()
                .showtimeId(data.showtime().getId())
                .seatIds(selectedSeatIds)
                .build());
        BookingResponse retry = bookingService.createBooking(CreateBookingRequest.builder()
                .showtimeId(data.showtime().getId())
                .seatIds(selectedSeatIds)
                .build());

        assertThat(retry.getId()).isEqualTo(first.getId());
        assertThat(bookingRepository.findAll())
                .filteredOn(booking -> booking.getStatus() == BookingStatus.PENDING)
                .hasSize(1);
    }

    @Test
    void databaseShouldRejectConcurrentPendingBookingsForTheSameUserAndShowtime() {
        TestShowtimeData data = createShowtimeData();
        Booking first = bookingRepository.saveAndFlush(Booking.builder()
                .user(customer)
                .showtime(data.showtime())
                .totalPrice(BASE_PRICE)
                .discountAmount(BigDecimal.ZERO)
                .status(BookingStatus.PENDING)
                .secureToken("pending-booking-one-" + UUID.randomUUID())
                .paymentExpiresAt(LocalDateTime.now().plusMinutes(5))
                .build());

        assertThatThrownBy(() -> bookingRepository.saveAndFlush(Booking.builder()
                .user(customer)
                .showtime(data.showtime())
                .totalPrice(BASE_PRICE)
                .discountAmount(BigDecimal.ZERO)
                .status(BookingStatus.PENDING)
                .secureToken("pending-booking-two-" + UUID.randomUUID())
                .paymentExpiresAt(LocalDateTime.now().plusMinutes(5))
                .build()))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(bookingRepository.findById(first.getId())).isPresent();
    }

    @Test
    void databaseShouldRejectTwoPendingPaymentsForOneBookingAcrossGateways() {
        TestShowtimeData data = createShowtimeData();
        Booking booking = bookingRepository.saveAndFlush(Booking.builder()
                .user(customer)
                .showtime(data.showtime())
                .totalPrice(BASE_PRICE)
                .discountAmount(BigDecimal.ZERO)
                .status(BookingStatus.PENDING)
                .secureToken("pending-payment-" + UUID.randomUUID())
                .paymentExpiresAt(LocalDateTime.now().plusMinutes(5))
                .build());

        paymentRepository.saveAndFlush(Payment.builder()
                .booking(booking)
                .amount(BASE_PRICE)
                .method(PaymentMethod.VNPAY)
                .transactionNo("VNPAY-" + UUID.randomUUID())
                .status(PaymentStatus.PENDING)
                .build());

        assertThatThrownBy(() -> paymentRepository.saveAndFlush(Payment.builder()
                .booking(booking)
                .amount(BASE_PRICE)
                .method(PaymentMethod.SEPAY)
                .transactionNo("SEPAY-" + UUID.randomUUID())
                .status(PaymentStatus.PENDING)
                .build()))
                .isInstanceOf(DataIntegrityViolationException.class);
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
    void applyPromotion_shouldRecalculateTotalAndExpirePendingPayments() {
        TestShowtimeData data = createShowtimeData();
        authenticateAs(customer, "BOOKING_CREATE", "BOOKING_VIEW_OWN");
        List<UUID> selectedSeatIds = data.seats().stream().limit(2).map(Seat::getId).toList();

        bookingService.holdSeats(new HoldSeatRequest(data.showtime().getId(), selectedSeatIds));
        BookingResponse pendingBooking = bookingService.createBooking(CreateBookingRequest.builder()
                .showtimeId(data.showtime().getId())
                .seatIds(selectedSeatIds)
                .build());

        Payment stalePayment = paymentRepository.save(Payment.builder()
                .booking(bookingRepository.findWithDetailsById(pendingBooking.getId()).orElseThrow())
                .amount(pendingBooking.getTotalPrice())
                .method(PaymentMethod.VNPAY)
                .transactionNo("VNPAY_STALE_PROMO")
                .status(PaymentStatus.PENDING)
                .build());

        promotionRepository.save(Promotion.builder()
                .code("SAVE50K")
                .description("Discount for booking workflow integration test")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("50000.00"))
                .minOrderValue(BigDecimal.ZERO)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(1))
                .usageLimit(100)
                .usedCount(0)
                .isActive(true)
                .isDeleted(false)
                .build());

        BookingResponse discountedBooking = bookingService.applyPromotion(pendingBooking.getId(), "SAVE50K");

        assertThat(discountedBooking.getDiscountAmount()).isEqualByComparingTo("50000.00");
        assertThat(discountedBooking.getTotalPrice()).isEqualByComparingTo("150000.00");
        assertThat(paymentRepository.findById(stalePayment.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.EXPIRED);
    }

    @Test
    void expirePendingBooking_shouldMarkBookingExpiredReleaseSeatsAndExpirePendingPayments() {
        TestShowtimeData data = createShowtimeData();
        authenticateAs(customer, "BOOKING_CREATE");
        List<UUID> selectedSeatIds = data.seats().stream().limit(2).map(Seat::getId).toList();

        bookingService.holdSeats(new HoldSeatRequest(data.showtime().getId(), selectedSeatIds));
        BookingResponse pendingBooking = bookingService.createBooking(CreateBookingRequest.builder()
                .showtimeId(data.showtime().getId())
                .seatIds(selectedSeatIds)
                .build());

        com.cinema.booking.entity.Booking booking = bookingRepository
                .findWithDetailsById(pendingBooking.getId())
                .orElseThrow();
        booking.setPaymentExpiresAt(LocalDateTime.now().minusMinutes(1));
        bookingRepository.saveAndFlush(booking);

        List<SeatStatus> heldSeats = seatStatusRepository.findAllByShowtimeId(data.showtime().getId()).stream()
                .filter(seatStatus -> selectedSeatIds.contains(seatStatus.getSeat().getId()))
                .peek(seatStatus -> seatStatus.setHoldUntil(LocalDateTime.now().minusMinutes(1)))
                .toList();
        seatStatusRepository.saveAllAndFlush(heldSeats);

        Payment pendingPayment = paymentRepository.save(Payment.builder()
                .booking(booking)
                .amount(pendingBooking.getTotalPrice())
                .method(PaymentMethod.SEPAY)
                .transactionNo("SEPAY_EXPIRE_BOOKING")
                .status(PaymentStatus.PENDING)
                .build());

        BookingResponse expiredBooking = bookingService.expirePendingBooking(pendingBooking.getId());

        assertThat(expiredBooking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(bookingRepository.findById(pendingBooking.getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.EXPIRED);
        assertThat(paymentRepository.findById(pendingPayment.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.EXPIRED);
        assertThat(seatStatusRepository.findAllByShowtimeId(data.showtime().getId()))
                .filteredOn(seatStatus -> selectedSeatIds.contains(seatStatus.getSeat().getId()))
                .allSatisfy(seatStatus -> {
                    assertThat(seatStatus.getStatus()).isEqualTo(SeatStatusType.AVAILABLE);
                    assertThat(seatStatus.getHoldBy()).isNull();
                    assertThat(seatStatus.getHoldUntil()).isNull();
                });
        verify(seatStatusPublisher).publishBulk(
                eq(data.showtime().getId()),
                argThat(ids -> ids.size() == selectedSeatIds.size() && ids.containsAll(selectedSeatIds)),
                eq(SeatStatusType.AVAILABLE));
    }

    @Test
    void getSeatMap_shouldRemainReadOnlyWhenAnExpiredHoldAwaitsSchedulerCleanup() {
        TestShowtimeData data = createShowtimeData();
        Seat expiredHeldSeat = data.seats().getFirst();
        SeatStatus expiredSeatStatus = seatStatusFor(data.showtime(), expiredHeldSeat);
        expiredSeatStatus.setStatus(SeatStatusType.HOLD);
        expiredSeatStatus.setHoldBy(customer);
        expiredSeatStatus.setHoldUntil(LocalDateTime.now().minusMinutes(1));
        seatStatusRepository.saveAndFlush(expiredSeatStatus);

        List<com.cinema.booking.dto.response.SeatMapItemResponse> seatMap =
                bookingService.getSeatMap(data.showtime().getId());

        assertThat(seatMap)
                .filteredOn(item -> item.getSeatId().equals(expiredHeldSeat.getId()))
                .singleElement()
                .satisfies(item -> assertThat(item.getStatus()).isEqualTo(SeatStatusType.HOLD));
        SeatStatus heldSeat = seatStatusFor(data.showtime(), expiredHeldSeat);
        assertThat(heldSeat.getStatus()).isEqualTo(SeatStatusType.HOLD);
        assertThat(heldSeat.getHoldBy().getId()).isEqualTo(customer.getId());
        verifyNoInteractions(seatStatusPublisher);
    }

    @Test
    void expiredPaymentAttempt_shouldCommitBookingAndSeatCleanupBeforeReturningError() {
        TestShowtimeData data = createShowtimeData();
        authenticateAs(customer, "BOOKING_CREATE", "PAYMENT_CREATE");
        List<UUID> selectedSeatIds = List.of(data.seats().getFirst().getId());

        bookingService.holdSeats(new HoldSeatRequest(data.showtime().getId(), selectedSeatIds));
        BookingResponse pendingBooking = bookingService.createBooking(CreateBookingRequest.builder()
                .showtimeId(data.showtime().getId())
                .seatIds(selectedSeatIds)
                .build());

        Booking booking = bookingRepository.findWithDetailsById(pendingBooking.getId()).orElseThrow();
        booking.setPaymentExpiresAt(LocalDateTime.now().minusSeconds(1));
        bookingRepository.saveAndFlush(booking);

        assertThatThrownBy(() -> paymentService.initiatePayment(
                booking.getId(),
                PaymentMethod.SEPAY,
                booking.getTotalPrice(),
                new MockHttpServletRequest()))
                .isInstanceOfSatisfying(AppException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BOOKING_EXPIRED));

        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.EXPIRED);
        assertThat(seatStatusRepository.findAllByShowtimeId(data.showtime().getId()))
                .filteredOn(seatStatus -> selectedSeatIds.contains(seatStatus.getSeat().getId()))
                .allSatisfy(seatStatus -> {
                    assertThat(seatStatus.getStatus()).isEqualTo(SeatStatusType.AVAILABLE);
                    assertThat(seatStatus.getHoldBy()).isNull();
                    assertThat(seatStatus.getHoldUntil()).isNull();
                });
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
        staffCinemaRepository.save(StaffCinema.builder()
                .id(StaffCinemaId.builder()
                        .staffId(staff.getId())
                        .cinemaId(data.cinema().getId())
                        .build())
                .staff(staff)
                .cinema(data.cinema())
                .build());
        authenticateAs(staff, "ROLE_STAFF", "TICKET_CHECKIN");

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

    @Test
    void cancelShowtimeWithPolicy_shouldCancelBookingsTicketsReleaseSeatsAndRequestRefund() {
        TestShowtimeData data = createShowtimeData();
        authenticateAs(customer, "BOOKING_CREATE");
        List<UUID> selectedSeatIds = data.seats().stream().limit(2).map(Seat::getId).toList();

        bookingService.holdSeats(new HoldSeatRequest(data.showtime().getId(), selectedSeatIds));
        BookingResponse pendingBooking = bookingService.createBooking(CreateBookingRequest.builder()
                .showtimeId(data.showtime().getId())
                .seatIds(selectedSeatIds)
                .build());
        BookingResponse paidBooking = bookingService.handlePaymentSuccess(pendingBooking.getSecureToken());
        com.cinema.booking.entity.Booking booking = bookingRepository
                .findWithDetailsById(paidBooking.getId())
                .orElseThrow();
        Payment payment = paymentRepository.save(Payment.builder()
                .booking(booking)
                .amount(paidBooking.getTotalPrice())
                .method(PaymentMethod.SEPAY)
                .transactionNo("SEPAY_REFUND_REQUEST_TEST")
                .status(PaymentStatus.SUCCESS)
                .paymentTime(LocalDateTime.now())
                .build());

        authenticateAs(staff, "ROLE_STAFF", "SHOWTIME_UPDATE");
        staffCinemaRepository.save(StaffCinema.builder()
                .id(StaffCinemaId.builder()
                        .staffId(staff.getId())
                        .cinemaId(data.cinema().getId())
                        .build())
                .staff(staff)
                .cinema(data.cinema())
                .build());

        showtimeService.cancelShowtimeWithPolicy(
                data.showtime().getId(),
                ShowtimeCancelRequest.builder().reason("Projector maintenance").build());

        assertThat(showtimeRepository.findById(data.showtime().getId()).orElseThrow().getStatus())
                .isEqualTo(ShowtimeStatus.CANCELLED);
        assertThat(bookingRepository.findById(paidBooking.getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.REFUND_PENDING);
        assertThat(paymentRepository.findById(payment.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.REFUND_PENDING);
        assertThat(refundRepository.findAll())
                .singleElement()
                .satisfies(refund -> {
                    assertThat(refund.getBooking().getId()).isEqualTo(paidBooking.getId());
                    assertThat(refund.getPayment().getId()).isEqualTo(payment.getId());
                    assertThat(refund.getAmount()).isEqualByComparingTo(paidBooking.getTotalPrice());
                    assertThat(refund.getStatus()).isEqualTo(RefundStatus.PENDING);
                });
        assertThat(ticketRepository.findAll())
                .allSatisfy(ticket -> assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED));
        assertThat(seatStatusRepository.findAllByShowtimeId(data.showtime().getId()))
                .filteredOn(seatStatus -> selectedSeatIds.contains(seatStatus.getSeat().getId()))
                .allSatisfy(seatStatus -> assertThat(seatStatus.getStatus()).isEqualTo(SeatStatusType.AVAILABLE));
        assertThat(paymentEventRepository.findAll())
                .anySatisfy(event -> {
                    assertThat(event.getEventType()).isEqualTo(PaymentEventType.REFUND_REQUESTED);
                    assertThat(event.getBookingId()).isEqualTo(paidBooking.getId());
                    assertThat(event.getPaymentId()).isEqualTo(payment.getId());
                });

        authenticateAs(staff, "ROLE_ADMIN", "PAYMENT_REFUND");
        RefundResponse completedRefund = refundService.markRefunded(
                refundRepository.findAll().getFirst().getId(),
                RefundCompleteRequest.builder()
                        .providerRefundId("MANUAL_REFUND_001")
                        .note("Refunded by bank transfer")
                        .build());

        assertThat(completedRefund.getStatus()).isEqualTo(RefundStatus.SUCCESS);
        assertThat(bookingRepository.findById(paidBooking.getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.REFUNDED);
        assertThat(paymentRepository.findById(payment.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.REFUNDED);
        assertThat(paymentEventRepository.findAll())
                .anySatisfy(event -> {
                    assertThat(event.getEventType()).isEqualTo(PaymentEventType.REFUND_COMPLETED);
                    assertThat(event.getBookingId()).isEqualTo(paidBooking.getId());
                    assertThat(event.getPaymentId()).isEqualTo(payment.getId());
                });
        verify(emailService).sendShowtimeCancellationEmail(
                eq(paidBooking.getId()),
                eq("Projector maintenance"));
        verify(seatStatusPublisher).publishBulk(
                eq(data.showtime().getId()),
                argThat(ids -> ids.size() == selectedSeatIds.size() && ids.containsAll(selectedSeatIds)),
                eq(SeatStatusType.AVAILABLE));
    }

    @Test
    void synchronizeCurrentStatuses_shouldMoveShowtimesByCurrentTimeAndKeepCancelledUntouched() {
        TestShowtimeData data = createShowtimeData();
        LocalDateTime now = LocalDateTime.now();

        Showtime ongoing = showtimeRepository.save(Showtime.builder()
                .movie(data.movie())
                .room(data.room())
                .startTime(now.minusMinutes(10))
                .endTime(now.plusMinutes(20))
                .basePrice(BASE_PRICE)
                .status(ShowtimeStatus.UPCOMING)
                .isDeleted(false)
                .build());
        Showtime ended = showtimeRepository.save(Showtime.builder()
                .movie(data.movie())
                .room(data.room())
                .startTime(now.minusHours(3))
                .endTime(now.minusHours(1))
                .basePrice(BASE_PRICE)
                .status(ShowtimeStatus.UPCOMING)
                .isDeleted(false)
                .build());
        Showtime cancelled = showtimeRepository.save(Showtime.builder()
                .movie(data.movie())
                .room(data.room())
                .startTime(now.minusHours(2))
                .endTime(now.minusHours(1))
                .basePrice(BASE_PRICE)
                .status(ShowtimeStatus.CANCELLED)
                .isDeleted(false)
                .build());
        showtimeRepository.flush();

        int updatedCount = showtimeStatusSyncService.synchronizeCurrentStatuses();

        assertThat(updatedCount).isGreaterThanOrEqualTo(2);
        assertThat(showtimeRepository.findById(ongoing.getId()).orElseThrow().getStatus())
                .isEqualTo(ShowtimeStatus.ONGOING);
        assertThat(showtimeRepository.findById(ended.getId()).orElseThrow().getStatus())
                .isEqualTo(ShowtimeStatus.ENDED);
        assertThat(showtimeRepository.findById(cancelled.getId()).orElseThrow().getStatus())
                .isEqualTo(ShowtimeStatus.CANCELLED);
    }

    private void clearBusinessData() {
        paymentEventRepository.deleteAllInBatch();
        refundRepository.deleteAllInBatch();
        ticketRepository.deleteAllInBatch();
        staffCinemaRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        bookingRepository.deleteAllInBatch();
        promotionRepository.deleteAllInBatch();
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
