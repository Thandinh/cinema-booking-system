package com.cinema.booking.service.impl;

import com.cinema.booking.dto.request.ShowtimeCreationRequest;
import com.cinema.booking.dto.request.ShowtimeCancelRequest;
import com.cinema.booking.dto.request.ShowtimeSearchRequest;
import com.cinema.booking.dto.request.ShowtimeUpdateRequest;
import com.cinema.booking.dto.response.HomeMovieShowtimesResponse;
import com.cinema.booking.dto.response.HomeShowtimeFeedResponse;
import com.cinema.booking.dto.response.HomeShowtimeItemResponse;
import com.cinema.booking.dto.response.ShowtimeResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.Movie;
import com.cinema.booking.entity.Payment;
import com.cinema.booking.entity.Room;
import com.cinema.booking.entity.Seat;
import com.cinema.booking.entity.SeatStatus;
import com.cinema.booking.entity.Showtime;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.enums.PaymentEventType;
import com.cinema.booking.enums.PaymentStatus;
import com.cinema.booking.enums.SeatStatusType;
import com.cinema.booking.enums.ShowtimeStatus;
import com.cinema.booking.enums.TicketStatus;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.mapper.ShowtimeMapper;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.HomeShowtimeProjection;
import com.cinema.booking.repository.MovieRepository;
import com.cinema.booking.repository.PaymentRepository;
import com.cinema.booking.repository.PromotionRepository;
import com.cinema.booking.repository.RoomRepository;
import com.cinema.booking.repository.SeatRepository;
import com.cinema.booking.repository.SeatStatusRepository;
import com.cinema.booking.repository.ShowtimeRepository;
import com.cinema.booking.repository.TicketRepository;
import com.cinema.booking.service.EmailService;
import com.cinema.booking.service.PaymentEventService;
import com.cinema.booking.service.RefundService;
import com.cinema.booking.service.StaffCinemaScopeService;
import com.cinema.booking.service.ShowtimeService;
import com.cinema.booking.service.ShowtimeStatusSyncService;
import com.cinema.booking.specification.ShowtimeSpecifications;
import com.cinema.booking.websocket.SeatStatusPublisher;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ShowtimeServiceImpl implements ShowtimeService {

    ShowtimeRepository showtimeRepository;
    MovieRepository movieRepository;
    RoomRepository roomRepository;
    SeatRepository seatRepository;
    SeatStatusRepository seatStatusRepository;
    ShowtimeMapper showtimeMapper;
    StaffCinemaScopeService staffCinemaScopeService;
    BookingRepository bookingRepository;
    PaymentRepository paymentRepository;
    PromotionRepository promotionRepository;
    TicketRepository ticketRepository;
    PaymentEventService paymentEventService;
    RefundService refundService;
    EmailService emailService;
    SeatStatusPublisher seatStatusPublisher;
    ShowtimeStatusSyncService showtimeStatusSyncService;

    @NonFinal
    @Value("${showtime.public-days-ahead:7}")
    int publicDaysAhead;

    @NonFinal
    @Value("${showtime.booking-cutoff-minutes:15}")
    int bookingCutoffMinutes;

    @NonFinal
    @Value("${ticket.check-in-early-minutes:30}")
    int checkInEarlyMinutes;

    @NonFinal
    @Value("${ticket.check-in-late-minutes:30}")
    int checkInLateMinutes;

    @Override
    @Transactional
    public ShowtimeResponse createShowtime(ShowtimeCreationRequest request) {
        if (request.getEndTime().isBefore(request.getStartTime()) || request.getEndTime().isEqual(request.getStartTime())) {
            throw new AppException(ErrorCode.SHOWTIME_END_TIME_INVALID);
        }

        Movie movie = movieRepository.findById(request.getMovieId())
                .filter(m -> !m.getIsDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_FOUND));

        Room room = roomRepository.findActiveById(request.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        staffCinemaScopeService.validateCurrentStaffCanAccessCinema(room.getCinema().getId());

        // Nâng cấp: Tính thêm 15 phút dọn phòng (Cleaning buffer)
        LocalDateTime startTimeCheck = request.getStartTime().minusMinutes(15);
        LocalDateTime endTimeCheck = request.getEndTime().plusMinutes(15);

        if (showtimeRepository.isTimeOverlapping(room.getId(), startTimeCheck, endTimeCheck)) {
            throw new AppException(ErrorCode.SHOWTIME_TIME_OVERLAPPING);
        }

        Showtime showtime = showtimeMapper.toShowtime(request, movie, room);
        Showtime savedShowtime = showtimeRepository.save(showtime);

        // Tự động clone ghế phòng vào seat_status
        List<Seat> roomSeats = seatRepository.findActiveByRoomId(room.getId());
        if (!roomSeats.isEmpty()) {
            List<SeatStatus> seatStatuses = roomSeats.stream()
                    .map(seat -> SeatStatus.builder()
                            .seat(seat)
                            .showtime(savedShowtime)
                            .status(SeatStatusType.AVAILABLE)
                            .build())
                    .collect(Collectors.toList());
            seatStatusRepository.saveAll(seatStatuses);
        }

        log.info("Created showtime id={} for room={}, auto-generated {} seats", 
                savedShowtime.getId(), room.getName(), roomSeats.size());
        
        return showtimeMapper.toShowtimeResponse(savedShowtime);
    }

    @Override
    @Transactional
    public ShowtimeResponse updateShowtime(UUID id, ShowtimeUpdateRequest request) {
        Showtime showtime = showtimeRepository.findActiveById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHOWTIME_NOT_FOUND));
        staffCinemaScopeService.validateCurrentStaffCanAccessCinema(showtime.getRoom().getCinema().getId());

        LocalDateTime newStartTime = request.getStartTime() != null ? request.getStartTime() : showtime.getStartTime();
        LocalDateTime newEndTime = request.getEndTime() != null ? request.getEndTime() : showtime.getEndTime();

        if (newEndTime.isBefore(newStartTime) || newEndTime.isEqual(newStartTime)) {
            throw new AppException(ErrorCode.SHOWTIME_END_TIME_INVALID);
        }

        // Kiểm tra overlap (trừ suất hiện tại) nếu có đổi thời gian, bao gồm 15 phút dọn phòng
        if (!newStartTime.equals(showtime.getStartTime()) || !newEndTime.equals(showtime.getEndTime())) {
            LocalDateTime startTimeCheck = newStartTime.minusMinutes(15);
            LocalDateTime endTimeCheck = newEndTime.plusMinutes(15);

            if (showtimeRepository.isTimeOverlappingExclude(showtime.getRoom().getId(), startTimeCheck, endTimeCheck, id)) {
                throw new AppException(ErrorCode.SHOWTIME_TIME_OVERLAPPING);
            }
        }

        showtimeMapper.updateShowtime(showtime, request);
        Showtime saved = showtimeRepository.save(showtime);
        log.info("Updated showtime id={}", id);
        return showtimeMapper.toShowtimeResponse(saved);
    }

    @Override
    @Transactional
    public void deleteShowtime(UUID id) {
        Showtime showtime = showtimeRepository.findActiveById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHOWTIME_NOT_FOUND));
        staffCinemaScopeService.validateCurrentStaffCanAccessCinema(showtime.getRoom().getCinema().getId());

        if (bookingRepository.existsProtectedBookingForShowtime(
                id,
                List.of(BookingStatus.PENDING, BookingStatus.SUCCESS))) {
            throw new AppException(ErrorCode.SHOWTIME_HAS_ACTIVE_BOOKINGS);
        }
        
        showtime.setStatus(ShowtimeStatus.CANCELLED);
        showtime.setIsDeleted(true);
        showtimeRepository.save(showtime);
        
        // Keep seat_status rows for audit/history. They are hidden through showtime.isDeleted.

        log.info("Cancelled and soft-deleted showtime id={}", id);
    }

    @Override
    @Transactional
    public ShowtimeResponse cancelShowtimeWithPolicy(UUID id, ShowtimeCancelRequest request) {
        Showtime showtime = showtimeRepository.findActiveById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHOWTIME_NOT_FOUND));
        staffCinemaScopeService.validateCurrentStaffCanAccessCinema(showtime.getRoom().getCinema().getId());

        if (showtime.getStatus() != ShowtimeStatus.UPCOMING && showtime.getStatus() != ShowtimeStatus.ONGOING) {
            throw new AppException(ErrorCode.SHOWTIME_NOT_CANCELLABLE);
        }

        if (ticketRepository.existsByShowtimeIdAndStatus(id, TicketStatus.USED)) {
            throw new AppException(ErrorCode.SHOWTIME_HAS_USED_TICKETS);
        }

        String reason = request.getReason().trim();
        List<Booking> affectedBookings = bookingRepository.findWithDetailsByShowtimeIdAndStatuses(
                id,
                List.of(BookingStatus.PENDING, BookingStatus.SUCCESS));

        List<UUID> affectedBookingIds = affectedBookings.stream()
                .map(Booking::getId)
                .toList();
        List<Payment> successPayments = affectedBookingIds.isEmpty()
                ? List.of()
                : paymentRepository.findWithBookingByBookingIdInAndStatus(affectedBookingIds, PaymentStatus.SUCCESS);
        List<Payment> pendingPayments = affectedBookingIds.isEmpty()
                ? List.of()
                : paymentRepository.findWithBookingByBookingIdInAndStatus(affectedBookingIds, PaymentStatus.PENDING);

        if (!affectedBookingIds.isEmpty()) {
            promotionRepository.releaseReservationsForBookingIds(affectedBookingIds);
            affectedBookings.forEach(booking -> booking.setPromotionReserved(false));
        }

        Map<UUID, List<Payment>> successPaymentsByBookingId = successPayments.stream()
                .filter(payment -> payment.getBooking() != null)
                .collect(Collectors.groupingBy(payment -> payment.getBooking().getId()));

        for (Booking booking : affectedBookings) {
            BookingStatus beforeStatus = booking.getStatus();
            booking.setStatus(beforeStatus == BookingStatus.SUCCESS
                    ? BookingStatus.REFUND_PENDING
                    : BookingStatus.CANCELLED);

            List<UUID> seatIds = booking.getBookingDetails().stream()
                    .map(detail -> detail.getSeat().getId())
                    .toList();
            if (!seatIds.isEmpty()) {
                seatStatusRepository.releaseSeatsForCancelledShowtime(id, seatIds, SeatStatusType.AVAILABLE);
                publishSeatAvailabilityAfterCommit(id, seatIds);
            }

            booking.getBookingDetails().forEach(detail -> {
                if (detail.getTicket() != null && detail.getTicket().getStatus() == TicketStatus.ACTIVE) {
                    detail.getTicket().setStatus(TicketStatus.CANCELLED);
                }
            });

            if (beforeStatus == BookingStatus.SUCCESS) {
                successPaymentsByBookingId.getOrDefault(booking.getId(), List.of())
                        .forEach(payment -> recordRefundRequested(payment, booking, reason, showtime));
                sendCancellationEmailAfterCommit(booking.getId(), reason);
            }
        }

        pendingPayments.forEach(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            recordPendingPaymentCancelled(payment, reason, showtime);
        });
        paymentRepository.saveAll(successPayments);
        paymentRepository.saveAll(pendingPayments);
        bookingRepository.saveAll(affectedBookings);

        showtime.setStatus(ShowtimeStatus.CANCELLED);
        Showtime saved = showtimeRepository.save(showtime);

        log.info("Cancelled showtime id={} with policy. affectedBookings={}, refundRequests={}",
                id, affectedBookings.size(), successPayments.size());
        return showtimeMapper.toShowtimeResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShowtimeResponse> getAllShowtimes(ShowtimeSearchRequest request, Pageable pageable) {
        showtimeStatusSyncService.synchronizeCurrentStatuses();
        ShowtimeSearchRequest safeRequest = request != null ? request : new ShowtimeSearchRequest();
        LocalDateTime fromTime = safeRequest.getFromDate() != null ? safeRequest.getFromDate().atStartOfDay() : null;
        LocalDateTime toTime = safeRequest.getToDate() != null ? safeRequest.getToDate().plusDays(1).atStartOfDay() : null;
        String city = blankToNull(safeRequest.getCity());
        String keyword = normalizeKeyword(safeRequest.getKeyword());
        List<UUID> scopedCinemaIds = null;

        if (fromTime != null && toTime != null && !toTime.isAfter(fromTime)) {
            return Page.empty(pageable);
        }

        if (staffCinemaScopeService.isStaffButNotAdmin()) {
            scopedCinemaIds = staffCinemaScopeService.getCurrentStaffCinemaIds();
            if (scopedCinemaIds.isEmpty()) {
                return Page.empty(pageable);
            }
        }

        return showtimeRepository.findAll(
                        ShowtimeSpecifications.searchActive(
                                fromTime,
                                toTime,
                                city,
                                safeRequest.getCinemaId(),
                                safeRequest.getRoomId(),
                                safeRequest.getStatus(),
                                keyword,
                                scopedCinemaIds),
                        pageable)
                .map(showtimeMapper::toShowtimeResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public HomeShowtimeFeedResponse getHomeShowtimes(
            String city,
            UUID cinemaId,
            LocalDate requestedDate,
            int movieLimit,
            int showtimeLimit) {
        String normalizedCity = blankToNull(city);
        int safeDaysAhead = Math.max(1, publicDaysAhead);
        int safeMovieLimit = Math.max(1, Math.min(movieLimit, 10));
        int safeShowtimeLimit = Math.max(1, Math.min(showtimeLimit, 8));
        LocalDate today = LocalDate.now();
        LocalDate lastDateExclusive = today.plusDays(safeDaysAhead);
        LocalDateTime bookingFromTime = LocalDateTime.now().plusMinutes(Math.max(0, bookingCutoffMinutes));
        LocalDateTime publicWindowEnd = lastDateExclusive.atStartOfDay();
        List<LocalDate> availableDates = IntStream.range(0, safeDaysAhead)
                .mapToObj(today::plusDays)
                .toList();

        if (normalizedCity == null || cinemaId == null) {
            return buildHomeShowtimeFeed("", requestedDate != null ? requestedDate : today, availableDates, List.of());
        }

        LocalDate selectedDate = requestedDate;
        if (selectedDate == null) {
            LocalDateTime firstBookableStart = showtimeRepository.findFirstBookableStartByCinema(
                    normalizedCity,
                    cinemaId,
                    bookingFromTime,
                    publicWindowEnd);
            selectedDate = firstBookableStart != null ? firstBookableStart.toLocalDate() : today;
        }

        if (selectedDate.isBefore(today) || !selectedDate.isBefore(lastDateExclusive)) {
            return buildHomeShowtimeFeed(normalizedCity, selectedDate, availableDates, List.of());
        }

        LocalDateTime selectedDayStart = selectedDate.atStartOfDay();
        LocalDateTime fromTime = selectedDayStart.isAfter(bookingFromTime)
                ? selectedDayStart
                : bookingFromTime;
        LocalDateTime toTime = selectedDate.plusDays(1).atStartOfDay();
        if (!toTime.isAfter(fromTime)) {
            return buildHomeShowtimeFeed(normalizedCity, selectedDate, availableDates, List.of());
        }

        List<HomeShowtimeProjection> rows = showtimeRepository.findHomeShowtimeFeed(
                normalizedCity,
                cinemaId,
                fromTime,
                toTime,
                safeMovieLimit,
                safeShowtimeLimit);

        Map<UUID, List<HomeShowtimeProjection>> rowsByMovie = rows.stream()
                .collect(Collectors.groupingBy(
                        HomeShowtimeProjection::getMovieId,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<HomeMovieShowtimesResponse> movies = rowsByMovie.values().stream()
                .map(movieRows -> {
                    HomeShowtimeProjection first = movieRows.getFirst();
                    List<HomeShowtimeItemResponse> showtimes = movieRows.stream()
                            .map(row -> HomeShowtimeItemResponse.builder()
                                    .id(row.getShowtimeId())
                                    .cinemaId(row.getCinemaId())
                                    .cinemaName(row.getCinemaName())
                                    .roomId(row.getRoomId())
                                    .roomName(row.getRoomName())
                                    .startTime(row.getStartTime())
                                    .endTime(row.getEndTime())
                                    .basePrice(row.getBasePrice())
                                    .build())
                            .toList();

                    return HomeMovieShowtimesResponse.builder()
                            .movieId(first.getMovieId())
                            .movieTitle(first.getMovieTitle())
                            .posterUrl(first.getPosterUrl())
                            .ageRating(first.getAgeRating())
                            .duration(first.getMovieDuration())
                            .showtimes(showtimes)
                            .build();
                })
                .toList();

        return buildHomeShowtimeFeed(normalizedCity, selectedDate, availableDates, movies);
    }

    @Override
    @Transactional(readOnly = true)
    public ShowtimeResponse getShowtimeById(UUID id) {
        return showtimeRepository.findActiveById(id)
                .map(showtimeMapper::toShowtimeResponse)
                .orElseThrow(() -> new AppException(ErrorCode.SHOWTIME_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getShowtimesByMovieId(UUID movieId) {
        ShowtimeSearchWindow window = getPublicShowtimeWindow();
        return showtimeRepository.findBookableByMovieId(movieId, window.fromTime(), window.toTime()).stream()
                .map(showtimeMapper::toShowtimeResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShowtimeResponse> getShowtimesByCinemaId(UUID cinemaId, Pageable pageable) {
        ShowtimeSearchWindow window = getPublicShowtimeWindow();
        return showtimeRepository.findBookableByCinemaId(cinemaId, window.fromTime(), window.toTime(), pageable)
                .map(showtimeMapper::toShowtimeResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getOpenCheckInShowtimes(UUID cinemaId) {
        staffCinemaScopeService.validateCurrentStaffCanAccessCinema(cinemaId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime earliestStartTime = now.minusMinutes(Math.max(0, checkInLateMinutes));
        LocalDateTime latestStartTime = now.plusMinutes(Math.max(0, checkInEarlyMinutes));

        return showtimeRepository.findOpenForCheckIn(cinemaId, earliestStartTime, latestStartTime).stream()
                .map(showtimeMapper::toShowtimeResponse)
                .toList();
    }

    private HomeShowtimeFeedResponse buildHomeShowtimeFeed(
            String city,
            LocalDate selectedDate,
            List<LocalDate> availableDates,
            List<HomeMovieShowtimesResponse> movies) {
        return HomeShowtimeFeedResponse.builder()
                .city(city)
                .selectedDate(selectedDate)
                .availableDates(availableDates)
                .movies(movies)
                .build();
    }

    private String blankToNull(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeKeyword(String value) {
        String trimmed = blankToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private void recordRefundRequested(Payment payment, Booking booking, String reason, Showtime showtime) {
        PaymentStatus beforePaymentStatus = payment.getStatus();
        payment.setStatus(PaymentStatus.REFUND_PENDING);
        refundService.requestRefund(payment, booking, reason);

        paymentEventService.record(
                payment,
                booking,
                PaymentEventType.REFUND_REQUESTED,
                beforePaymentStatus,
                PaymentStatus.REFUND_PENDING,
                BookingStatus.SUCCESS,
                BookingStatus.REFUND_PENDING,
                true,
                "Showtime cancelled. Refund request was created.",
                cancellationPayload(reason, showtime));
    }

    private void recordPendingPaymentCancelled(Payment payment, String reason, Showtime showtime) {
        paymentEventService.record(
                payment,
                payment.getBooking(),
                PaymentEventType.PAYMENT_FAILED,
                PaymentStatus.PENDING,
                PaymentStatus.FAILED,
                BookingStatus.PENDING,
                BookingStatus.CANCELLED,
                false,
                "Pending payment was cancelled because showtime was cancelled.",
                cancellationPayload(reason, showtime));
    }

    private Map<String, Object> cancellationPayload(String reason, Showtime showtime) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reason", reason);
        payload.put("showtimeId", showtime.getId().toString());
        payload.put("movieTitle", showtime.getMovie().getTitle());
        payload.put("cinemaName", showtime.getRoom().getCinema().getName());
        payload.put("roomName", showtime.getRoom().getName());
        payload.put("startTime", showtime.getStartTime().toString());
        return payload;
    }

    private void publishSeatAvailabilityAfterCommit(UUID showtimeId, List<UUID> seatIds) {
        if (seatIds.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            seatStatusPublisher.publishBulk(showtimeId, seatIds, SeatStatusType.AVAILABLE);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                seatStatusPublisher.publishBulk(showtimeId, seatIds, SeatStatusType.AVAILABLE);
            }
        });
    }

    private void sendCancellationEmailAfterCommit(UUID bookingId, String reason) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            emailService.sendShowtimeCancellationEmail(bookingId, reason);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                emailService.sendShowtimeCancellationEmail(bookingId, reason);
            }
        });
    }

    private ShowtimeSearchWindow getPublicShowtimeWindow() {
        int safeCutoffMinutes = Math.max(0, bookingCutoffMinutes);
        int safeDaysAhead = Math.max(1, publicDaysAhead);
        LocalDateTime fromTime = LocalDateTime.now().plusMinutes(safeCutoffMinutes);
        LocalDateTime toTime = LocalDate.now().plusDays(safeDaysAhead).atStartOfDay();
        return new ShowtimeSearchWindow(fromTime, toTime);
    }

    private record ShowtimeSearchWindow(LocalDateTime fromTime, LocalDateTime toTime) {
    }
}
