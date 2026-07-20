package com.cinema.booking.service.impl;

import com.cinema.booking.dto.request.CreateBookingRequest;
import com.cinema.booking.dto.request.HoldSeatRequest;
import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.dto.response.HoldSeatResponse;
import com.cinema.booking.dto.response.SeatMapItemResponse;
import com.cinema.booking.dto.response.TicketResponse;
import com.cinema.booking.entity.*;
import com.cinema.booking.enums.*;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.mapper.BookingMapper;
import com.cinema.booking.mapper.TicketMapper;
import com.cinema.booking.repository.*;
import com.cinema.booking.service.BookingService;
import com.cinema.booking.service.EmailService;
import com.cinema.booking.service.TicketQrCodeService;
import com.cinema.booking.util.SecurityUtils;
import com.cinema.booking.websocket.SeatStatusPublisher;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BookingServiceImpl implements BookingService {

    BookingRepository    bookingRepository;
    SeatStatusRepository seatStatusRepository;
    SeatRepository       seatRepository;
    ShowtimeRepository   showtimeRepository;
    UserRepository       userRepository;
    PromotionRepository  promotionRepository;
    TicketRepository     ticketRepository;
    BookingMapper        bookingMapper;
    TicketMapper         ticketMapper;
    SeatStatusPublisher  seatStatusPublisher; // WebSocket real-time push
    EmailService         emailService;        // Gửi email vé
    TicketQrCodeService  ticketQrCodeService;
    PaymentRepository    paymentRepository;

    @Value("${ticket.check-in-early-minutes:30}")
    @NonFinal
    int checkInEarlyMinutes;

    @Value("${booking.pending-timeout-minutes:5}")
    @NonFinal
    int bookingPendingTimeoutMinutes;

    @Value("${booking.seat-hold-minutes:${booking.pending-timeout-minutes:5}}")
    @NonFinal
    int seatHoldMinutes;

    // =========================================================================
    // BƯỚC 1: GIỮ GHẾ
    // =========================================================================
    @Override
    @Transactional
    public HoldSeatResponse holdSeats(HoldSeatRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();

        // Lấy showtime để tính giá
        Showtime showtime = showtimeRepository.findActiveById(request.getShowtimeId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOWTIME_NOT_FOUND));

        if (showtime.getStatus() != ShowtimeStatus.UPCOMING && showtime.getStatus() != ShowtimeStatus.ONGOING) {
            throw new AppException(ErrorCode.SHOWTIME_NOT_BOOKABLE);
        }

        // Load các SeatStatus với PESSIMISTIC_WRITE lock — đây là điểm chống race condition
        List<SeatStatus> seatStatuses = seatStatusRepository.findForUpdateByShowtimeAndSeats(
                request.getShowtimeId(), request.getSeatIds());

        // Kiểm tra tìm đủ ghế
        if (seatStatuses.size() != request.getSeatIds().size()) {
            throw new AppException(ErrorCode.SEAT_NOT_FOUND);
        }

        // Kiểm tra từng ghế phải AVAILABLE
        boolean hasUnavailable = seatStatuses.stream()
                .anyMatch(ss -> ss.getStatus() != SeatStatusType.AVAILABLE);
        if (hasUnavailable) {
            throw new AppException(ErrorCode.SEAT_NOT_AVAILABLE);
        }

        // Đặt HOLD + tính giá ước tính
        LocalDateTime holdUntil = LocalDateTime.now().plusMinutes(seatHoldMinutes);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        BigDecimal estimatedTotal = BigDecimal.ZERO;
        for (SeatStatus ss : seatStatuses) {
            ss.setStatus(SeatStatusType.HOLD);
            ss.setHoldBy(user);
            ss.setHoldUntil(holdUntil);
            // Tính giá ghế: basePrice * priceMultiplier của loại ghế
            BigDecimal seatPrice = showtime.getBasePrice()
                    .multiply(ss.getSeat().getPriceMultiplier())
                    .setScale(2, RoundingMode.HALF_UP);
            estimatedTotal = estimatedTotal.add(seatPrice);
        }
        seatStatusRepository.saveAll(seatStatuses);

        log.info("User {} held {} seats for showtime {}", userId, seatStatuses.size(), request.getShowtimeId());

        // ── WS: Push HOLD event xuống tất cả client đang xem sơ đồ ghế này ──
        publishHoldAfterCommit(
                request.getShowtimeId(),
                seatStatuses.stream().map(ss -> ss.getSeat().getId()).toList(),
                userId,
                holdUntil);

        return HoldSeatResponse.builder()
                .showtimeId(request.getShowtimeId())
                .heldSeatIds(seatStatuses.stream().map(ss -> ss.getSeat().getId()).toList())
                .holdUntil(holdUntil)
                .estimatedTotalPrice(estimatedTotal)
                .message("Ghế đã được giữ trong " + seatHoldMinutes + " phút")
                .build();
    }

    // =========================================================================
    // BƯỚC 2: TẠO BOOKING
    // =========================================================================
    @Override
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();

        Showtime showtime = showtimeRepository.findActiveById(request.getShowtimeId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOWTIME_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Xác minh lại ghế vẫn đang HOLD bởi chính user này
        List<SeatStatus> seatStatuses = seatStatusRepository.findForUpdateByShowtimeAndSeats(
                request.getShowtimeId(), request.getSeatIds());

        if (seatStatuses.size() != request.getSeatIds().size()) {
            throw new AppException(ErrorCode.SEAT_NOT_FOUND);
        }

        for (SeatStatus ss : seatStatuses) {
            // 1. Kiểm tra hold đã hết hạn trước — rõ ràng hơn cho client
            if (ss.getHoldUntil() != null && ss.getHoldUntil().isBefore(LocalDateTime.now())) {
                throw new AppException(ErrorCode.SEAT_HOLD_EXPIRED);
            }
            // 2. Ghế phải đang ở trạng thái HOLD
            if (ss.getStatus() != SeatStatusType.HOLD) {
                throw new AppException(ErrorCode.SEAT_NOT_HELD);
            }
            // 3. Ghế phải được giữ bởi chính user đang thao tác
            if (!userId.equals(ss.getHoldBy().getId())) {
                throw new AppException(ErrorCode.SEAT_HELD_BY_ANOTHER);
            }
        }

        // Tính tổng giá trước giảm
        BigDecimal totalBeforeDiscount = seatStatuses.stream()
                .map(ss -> showtime.getBasePrice()
                        .multiply(ss.getSeat().getPriceMultiplier())
                        .setScale(2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Áp dụng khuyến mãi (nếu có)
        Promotion promotion = null;
        BigDecimal discountAmount = BigDecimal.ZERO;

        if (request.getPromotionCode() != null && !request.getPromotionCode().isBlank()) {
            promotion = validateAndGetPromotion(request.getPromotionCode().trim().toUpperCase(), totalBeforeDiscount);
            discountAmount = calculateDiscount(promotion, totalBeforeDiscount);
        }

        BigDecimal totalPrice = totalBeforeDiscount.subtract(discountAmount).max(BigDecimal.ZERO);
        LocalDateTime paymentHoldUntil = LocalDateTime.now().plusMinutes(bookingPendingTimeoutMinutes);

        // Tạo Booking
        Booking booking = Booking.builder()
                .user(user)
                .showtime(showtime)
                .promotion(promotion)
                .totalPrice(totalPrice)
                .discountAmount(discountAmount)
                .status(BookingStatus.PENDING)
                .secureToken(UUID.randomUUID().toString())
                .paymentExpiresAt(paymentHoldUntil)
                .build();

        // Tạo BookingDetails
        List<BookingDetail> details = seatStatuses.stream()
                .map(ss -> BookingDetail.builder()
                        .booking(booking)
                        .seat(ss.getSeat())
                        .priceAtBooking(
                                showtime.getBasePrice()
                                        .multiply(ss.getSeat().getPriceMultiplier())
                                        .setScale(2, RoundingMode.HALF_UP))
                        .build())
                .toList();
        booking.getBookingDetails().addAll(details);

        Booking saved = bookingRepository.save(booking);

        for (SeatStatus ss : seatStatuses) {
            ss.setHoldUntil(paymentHoldUntil);
        }
        seatStatusRepository.saveAll(seatStatuses);

        log.info("Booking created id={} for user={}, total={}", saved.getId(), userId, totalPrice);

        return bookingMapper.toBookingResponse(saved);
    }

    // =========================================================================
    // BƯỚC 4a: THANH TOÁN THÀNH CÔNG (callback từ VNPay/MoMo)
    // =========================================================================
    @Override
    @Transactional
    public BookingResponse handlePaymentSuccess(String secureToken) {
        Booking booking = bookingRepository.findBySecureToken(secureToken)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new AppException(ErrorCode.BOOKING_ALREADY_PROCESSED);
        }

        if (isPaymentExpired(booking)) {
            throw new AppException(ErrorCode.BOOKING_EXPIRED);
        }

        booking.setStatus(BookingStatus.SUCCESS);

        // Bulk-update toàn bộ ghế → BOOKED (đồng thời xóa hold_by và hold_until)
        List<UUID> seatIds = booking.getBookingDetails().stream()
                .map(bd -> bd.getSeat().getId())
                .toList();
        seatStatusRepository.bulkUpdateStatusAndClearHold(booking.getShowtime().getId(), seatIds, SeatStatusType.BOOKED);

        // ── WS: Push BOOKED event ──
        publishBulkAfterCommit(booking.getShowtime().getId(), seatIds, SeatStatusType.BOOKED);

        // Sinh QR Ticket cho từng ghế
        for (BookingDetail detail : booking.getBookingDetails()) {
            String qrCode = ticketQrCodeService.generate(detail.getId());
            Ticket ticket = Ticket.builder()
                    .bookingDetail(detail)
                    .qrCode(qrCode)
                    .status(TicketStatus.ACTIVE)
                    .build();
            Ticket savedTicket = ticketRepository.save(ticket);
            detail.setTicket(savedTicket);
        }

        // Tăng used_count của promotion
        // Không cần gọi save() vì entity đang trong managed state của JPA
        if (booking.getPromotion() != null) {
            booking.getPromotion().setUsedCount(booking.getPromotion().getUsedCount() + 1);
        }

        Booking saved = bookingRepository.save(booking);
        log.info("Payment SUCCESS for booking id={}", saved.getId());
        
        // Gửi email sau khi transaction commit để thread async nhìn thấy đầy đủ ticket/QR trong DB.
        UUID bookingId = saved.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                emailService.sendTicketEmail(bookingId);
            }
        });
        
        return bookingMapper.toBookingResponse(saved);
    }

    // =========================================================================
    // BƯỚC 4b: THANH TOÁN THẤT BẠI (callback từ VNPay/MoMo)
    // =========================================================================
    @Override
    @Transactional
    public BookingResponse handlePaymentFailure(String secureToken) {
        Booking booking = bookingRepository.findBySecureToken(secureToken)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new AppException(ErrorCode.BOOKING_ALREADY_PROCESSED);
        }

        booking.setStatus(BookingStatus.FAILED);

        // Nhả ghế về AVAILABLE, đồng thời xóa thông tin hold
        List<UUID> seatIds = booking.getBookingDetails().stream()
                .map(bd -> bd.getSeat().getId())
                .toList();
        int releasedSeatCount = seatStatusRepository.releaseHeldSeatsForBooking(
                booking.getShowtime().getId(),
                seatIds,
                booking.getUser().getId(),
                paymentReleaseCutoff(booking),
                SeatStatusType.AVAILABLE);

        // ── WS: Push AVAILABLE event (ghế được trả lại) ──
        if (releasedSeatCount == seatIds.size()) {
            publishBulkAfterCommit(booking.getShowtime().getId(), seatIds, SeatStatusType.AVAILABLE);
        }

        Booking saved = bookingRepository.save(booking);
        log.info("Payment FAILED for booking id={}", saved.getId());
        return bookingMapper.toBookingResponse(saved);
    }

    // =========================================================================
    // HỦY BOOKING
    // =========================================================================
    @Override
    @Transactional
    public BookingResponse cancelBooking(UUID bookingId) {
        UUID userId = SecurityUtils.getCurrentUserId();

        Booking booking = bookingRepository.findWithDetailsById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        // User thường chỉ được hủy booking của mình
        boolean isOwner = booking.getUser().getId().equals(userId);
        boolean isAdmin = SecurityUtils.hasAuthority("BOOKING_CANCEL_ALL");

        if (!isOwner && !isAdmin) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new AppException(ErrorCode.BOOKING_CANNOT_CANCEL);
        }

        booking.setStatus(BookingStatus.CANCELLED);

        List<UUID> seatIds = booking.getBookingDetails().stream()
                .map(bd -> bd.getSeat().getId())
                .toList();
        int releasedSeatCount = seatStatusRepository.releaseHeldSeatsForBooking(
                booking.getShowtime().getId(),
                seatIds,
                booking.getUser().getId(),
                paymentReleaseCutoff(booking),
                SeatStatusType.AVAILABLE);

        // ── WS: Push AVAILABLE event (hủy đơn, trả ghế về pool) ──
        if (releasedSeatCount == seatIds.size()) {
            publishBulkAfterCommit(booking.getShowtime().getId(), seatIds, SeatStatusType.AVAILABLE);
        }

        Booking saved = bookingRepository.save(booking);
        log.info("Booking CANCELLED id={} by userId={}", bookingId, userId);
        return bookingMapper.toBookingResponse(saved);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BookingResponse expirePendingBooking(UUID bookingId) {
        Booking booking = bookingRepository.findWithDetailsById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (booking.getStatus() != BookingStatus.PENDING) {
            return bookingMapper.toBookingResponse(booking);
        }

        booking.setStatus(BookingStatus.EXPIRED);

        List<UUID> seatIds = booking.getBookingDetails().stream()
                .map(bd -> bd.getSeat().getId())
                .toList();

        if (!seatIds.isEmpty()) {
            int releasedSeatCount = seatStatusRepository.releaseHeldSeatsForBooking(
                    booking.getShowtime().getId(),
                    seatIds,
                    booking.getUser().getId(),
                    paymentReleaseCutoff(booking),
                    SeatStatusType.AVAILABLE);
            if (releasedSeatCount == seatIds.size()) {
                publishBulkAfterCommit(booking.getShowtime().getId(), seatIds, SeatStatusType.AVAILABLE);
            }
        }

        List<Payment> pendingPayments = paymentRepository.findByBookingIdInAndStatus(
                List.of(booking.getId()), PaymentStatus.PENDING);
        pendingPayments.forEach(payment -> payment.setStatus(PaymentStatus.EXPIRED));
        paymentRepository.saveAll(pendingPayments);

        Booking saved = bookingRepository.save(booking);
        log.info("Booking EXPIRED id={} after payment timeout", bookingId);
        return bookingMapper.toBookingResponse(saved);
    }

    // =========================================================================
    // SƠ ĐỒ GHẾ
    // =========================================================================
    @Override
    @Transactional
    public List<SeatMapItemResponse> getSeatMap(UUID showtimeId) {
        Showtime showtime = showtimeRepository.findActiveById(showtimeId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOWTIME_NOT_FOUND));

        releaseExpiredHoldsForShowtime(showtimeId);

        List<SeatStatus> seatStatuses = seatStatusRepository.findAllByShowtimeId(showtimeId);
        long activeSeatCount = seatRepository.countActiveByRoomId(showtime.getRoom().getId());

        if (seatStatuses.size() < activeSeatCount) {
            seedMissingSeatStatuses(showtime, seatStatuses);
            seatStatuses = seatStatusRepository.findAllByShowtimeId(showtimeId);
        }

        return seatStatuses.stream()
                .map(bookingMapper::toSeatMapItemResponse)
                .toList();
    }

    // =========================================================================
    // DANH SÁCH BOOKING
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getMyBookings(BookingStatus status, Pageable pageable) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return mapBookingPage(bookingRepository.findIdsByUserIdAndStatus(userId, status, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getAllBookings(BookingStatus status, Pageable pageable) {
        return mapBookingPage(bookingRepository.findIdsByStatus(status, pageable));
    }

    private Page<BookingResponse> mapBookingPage(Page<UUID> bookingIdPage) {
        if (bookingIdPage.isEmpty()) {
            return new PageImpl<>(List.of(), bookingIdPage.getPageable(), bookingIdPage.getTotalElements());
        }

        List<UUID> ids = bookingIdPage.getContent();
        Map<UUID, Booking> bookingsById = bookingRepository.findAllWithDetailsByIdIn(ids).stream()
                .collect(Collectors.toMap(Booking::getId, Function.identity(), (left, right) -> left));

        List<BookingResponse> content = ids.stream()
                .map(bookingsById::get)
                .filter(Objects::nonNull)
                .map(bookingMapper::toBookingResponse)
                .toList();

        return new PageImpl<>(content, bookingIdPage.getPageable(), bookingIdPage.getTotalElements());
    }

    @Override
    @Transactional
    public BookingResponse getBookingById(UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Booking booking = bookingRepository.findWithDetailsById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        boolean isOwner = booking.getUser().getId().equals(userId);
        boolean canViewAll = SecurityUtils.hasAuthority("BOOKING_VIEW_ALL");
        if (!isOwner && !canViewAll) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        upgradeLegacyTicketQrCodes(booking);

        return bookingMapper.toBookingResponse(booking);
    }

    // =========================================================================
    // VÉ & CHECK-IN
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> getMyTickets(Pageable pageable) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ticketRepository.findByUserId(userId, pageable)
                .map(ticketMapper::toTicketResponse);
    }

    @Override
    @Transactional
    public TicketResponse checkInTicket(String qrCode) {
        String normalizedQrCode = ticketQrCodeService.normalizeAndValidate(qrCode);
        Ticket ticket = ticketRepository.findByQrCodeForCheckIn(normalizedQrCode)
                .orElseThrow(() -> new AppException(ErrorCode.TICKET_NOT_FOUND));

        if (ticket.getStatus() == TicketStatus.USED) {
            throw new AppException(ErrorCode.TICKET_ALREADY_USED);
        }
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new AppException(ErrorCode.TICKET_CANCELLED);
        }
        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            throw new AppException(ErrorCode.TICKET_NOT_ACTIVE);
        }

        Booking booking = ticket.getBookingDetail().getBooking();
        if (booking.getStatus() != BookingStatus.SUCCESS) {
            throw new AppException(ErrorCode.TICKET_NOT_ACTIVE);
        }

        LocalDateTime now = LocalDateTime.now();
        Showtime showtime = booking.getShowtime();
        if (now.isBefore(showtime.getStartTime().minusMinutes(checkInEarlyMinutes))) {
            throw new AppException(ErrorCode.TICKET_CHECKIN_TOO_EARLY);
        }
        if (now.isAfter(showtime.getEndTime())) {
            throw new AppException(ErrorCode.TICKET_CHECKIN_EXPIRED);
        }

        ticket.setStatus(TicketStatus.USED);
        ticket.setCheckInTime(now);
        ticketRepository.save(ticket);

        log.info("Ticket checked in id={} bookingId={}", ticket.getId(), booking.getId());
        return ticketMapper.toTicketResponse(ticket);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Xác thực mã khuyến mãi có hợp lệ và còn dùng được không.
     */
    private Promotion validateAndGetPromotion(String code, BigDecimal orderValue) {
        Promotion promo = promotionRepository.findActiveByCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));

        if (!promo.getIsActive()) {
            throw new AppException(ErrorCode.PROMOTION_NOT_ACTIVE);
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(promo.getStartDate()) || now.isAfter(promo.getEndDate())) {
            throw new AppException(ErrorCode.PROMOTION_EXPIRED);
        }

        if (promo.getUsageLimit() != null && promo.getUsedCount() >= promo.getUsageLimit()) {
            throw new AppException(ErrorCode.PROMOTION_LIMIT_REACHED);
        }

        if (orderValue.compareTo(promo.getMinOrderValue()) < 0) {
            throw new AppException(ErrorCode.PROMOTION_MIN_ORDER_NOT_MET);
        }

        return promo;
    }
    /**
     * Tính tiền giảm theo loại PERCENT hoặc FIXED.
     * Áp dụng maxDiscountAmount nếu có (chỉ với PERCENT).
     */
    private BigDecimal calculateDiscount(Promotion promo, BigDecimal orderValue) {
        if (promo.getDiscountType() == DiscountType.PERCENT) {
            BigDecimal discount = orderValue
                    .multiply(promo.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            // Cap theo maxDiscountAmount nếu admin đặt giới hạn
            if (promo.getMaxDiscountAmount() != null) {
                discount = discount.min(promo.getMaxDiscountAmount());
            }
            return discount;
        } else {
            // FIXED: giảm một khoản cố định, không vượt quá tổng đơn hàng
            return promo.getDiscountValue().min(orderValue);
        }
    }

    private void releaseExpiredHoldsForShowtime(UUID showtimeId) {
        List<ExpiredSeatHoldProjection> expiredHolds = seatStatusRepository.findExpiredHoldRowsByShowtime(
                showtimeId, LocalDateTime.now());

        if (expiredHolds.isEmpty()) {
            return;
        }

        List<UUID> releasedSeatIds = expiredHolds.stream()
                .map(ExpiredSeatHoldProjection::getSeatId)
                .toList();

        List<UUID> expiredHoldIds = expiredHolds.stream()
                .map(ExpiredSeatHoldProjection::getId)
                .toList();

        int releasedCount = seatStatusRepository.releaseExpiredHoldsByIds(expiredHoldIds);
        publishBulkAfterCommit(showtimeId, releasedSeatIds, SeatStatusType.AVAILABLE);
        log.info("Released {} expired holds while loading seat map for showtime={}",
                releasedCount, showtimeId);
    }

    private void seedMissingSeatStatuses(Showtime showtime, List<SeatStatus> existingStatuses) {
        Set<UUID> existingSeatIds = existingStatuses.stream()
                .map(seatStatus -> seatStatus.getSeat().getId())
                .collect(Collectors.toSet());

        List<SeatStatus> missingStatuses = seatRepository.findActiveByRoomId(showtime.getRoom().getId()).stream()
                .filter(seat -> !existingSeatIds.contains(seat.getId()))
                .map(seat -> SeatStatus.builder()
                        .showtime(showtime)
                        .seat(seat)
                        .status(SeatStatusType.AVAILABLE)
                        .build())
                .toList();

        if (!missingStatuses.isEmpty()) {
            seatStatusRepository.saveAll(missingStatuses);
            log.warn("Auto-generated {} missing seat_status rows for showtime={}",
                    missingStatuses.size(), showtime.getId());
        }
    }

    private void upgradeLegacyTicketQrCodes(Booking booking) {
        if (booking.getStatus() != BookingStatus.SUCCESS) {
            return;
        }

        for (BookingDetail detail : booking.getBookingDetails()) {
            Ticket ticket = detail.getTicket();
            if (ticket != null && !ticketQrCodeService.isValidSignedToken(ticket.getQrCode())) {
                ticket.setQrCode(ticketQrCodeService.generate(detail.getId()));
                log.info("Upgraded legacy ticket QR id={} bookingId={}", ticket.getId(), booking.getId());
            }
        }
    }

    private boolean isPaymentExpired(Booking booking) {
        return booking.getPaymentExpiresAt() != null
                && !booking.getPaymentExpiresAt().isAfter(LocalDateTime.now());
    }

    private LocalDateTime paymentReleaseCutoff(Booking booking) {
        return booking.getPaymentExpiresAt() != null
                ? booking.getPaymentExpiresAt()
                : LocalDateTime.now();
    }

    private void publishBulkAfterCommit(UUID showtimeId, List<UUID> seatIds, SeatStatusType status) {
        if (seatIds.isEmpty()) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            seatStatusPublisher.publishBulk(showtimeId, seatIds, status);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                seatStatusPublisher.publishBulk(showtimeId, seatIds, status);
            }
        });
    }

    private void publishHoldAfterCommit(
            UUID showtimeId,
            List<UUID> seatIds,
            UUID userId,
            LocalDateTime holdUntil) {

        if (seatIds.isEmpty()) {
            return;
        }

        Runnable publish = () -> seatIds.forEach(seatId ->
                seatStatusPublisher.publishHold(showtimeId, seatId, userId, holdUntil));

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publish.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish.run();
            }
        });
    }

}
