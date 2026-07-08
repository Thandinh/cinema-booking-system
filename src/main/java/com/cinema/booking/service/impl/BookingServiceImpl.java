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
import com.cinema.booking.util.SecurityUtils;
import com.cinema.booking.websocket.SeatStatusPublisher;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BookingServiceImpl implements BookingService {

    static final int HOLD_MINUTES = 10;

    BookingRepository    bookingRepository;
    SeatStatusRepository seatStatusRepository;
    ShowtimeRepository   showtimeRepository;
    UserRepository       userRepository;
    PromotionRepository  promotionRepository;
    TicketRepository     ticketRepository;
    BookingMapper        bookingMapper;
    TicketMapper         ticketMapper;
    SeatStatusPublisher  seatStatusPublisher; // WebSocket real-time push
    EmailService         emailService;        // Gửi email vé

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
        LocalDateTime holdUntil = LocalDateTime.now().plusMinutes(HOLD_MINUTES);
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
        seatStatuses.forEach(ss ->
                seatStatusPublisher.publishHold(
                        request.getShowtimeId(),
                        ss.getSeat().getId(),
                        userId,
                        holdUntil));

        return HoldSeatResponse.builder()
                .showtimeId(request.getShowtimeId())
                .heldSeatIds(seatStatuses.stream().map(ss -> ss.getSeat().getId()).toList())
                .holdUntil(holdUntil)
                .estimatedTotalPrice(estimatedTotal)
                .message("Ghế đã được giữ trong " + HOLD_MINUTES + " phút")
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

        // Tạo Booking
        Booking booking = Booking.builder()
                .user(user)
                .showtime(showtime)
                .promotion(promotion)
                .totalPrice(totalPrice)
                .discountAmount(discountAmount)
                .status(BookingStatus.PENDING)
                .secureToken(UUID.randomUUID().toString())
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

        booking.setStatus(BookingStatus.SUCCESS);

        // Bulk-update toàn bộ ghế → BOOKED (đồng thời xóa hold_by và hold_until)
        List<UUID> seatIds = booking.getBookingDetails().stream()
                .map(bd -> bd.getSeat().getId())
                .toList();
        seatStatusRepository.bulkUpdateStatusAndClearHold(booking.getShowtime().getId(), seatIds, SeatStatusType.BOOKED);

        // ── WS: Push BOOKED event ──
        seatStatusPublisher.publishBulk(booking.getShowtime().getId(), seatIds, SeatStatusType.BOOKED);

        // Sinh QR Ticket cho từng ghế
        for (BookingDetail detail : booking.getBookingDetails()) {
            String qrCode = generateQrCode(booking.getId(), detail.getSeat().getId());
            Ticket ticket = Ticket.builder()
                    .bookingDetail(detail)
                    .qrCode(qrCode)
                    .status(TicketStatus.ACTIVE)
                    .build();
            detail.setTicket(ticket);
        }

        // Tăng used_count của promotion
        // Không cần gọi save() vì entity đang trong managed state của JPA
        if (booking.getPromotion() != null) {
            booking.getPromotion().setUsedCount(booking.getPromotion().getUsedCount() + 1);
        }

        Booking saved = bookingRepository.save(booking);
        log.info("Payment SUCCESS for booking id={}", saved.getId());
        
        // Gửi email bất đồng bộ — truyền UUID, không truyền entity
        // để tránh LazyInitializationException khi @Async chạy trong thread khác
        emailService.sendTicketEmail(saved.getId());
        
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
        seatStatusRepository.bulkUpdateStatusAndClearHold(booking.getShowtime().getId(), seatIds, SeatStatusType.AVAILABLE);

        // ── WS: Push AVAILABLE event (ghế được trả lại) ──
        seatStatusPublisher.publishBulk(booking.getShowtime().getId(), seatIds, SeatStatusType.AVAILABLE);

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
        seatStatusRepository.bulkUpdateStatusAndClearHold(booking.getShowtime().getId(), seatIds, SeatStatusType.AVAILABLE);

        // ── WS: Push AVAILABLE event (hủy đơn, trả ghế về pool) ──
        seatStatusPublisher.publishBulk(booking.getShowtime().getId(), seatIds, SeatStatusType.AVAILABLE);

        Booking saved = bookingRepository.save(booking);
        log.info("Booking CANCELLED id={} by userId={}", bookingId, userId);
        return bookingMapper.toBookingResponse(saved);
    }

    // =========================================================================
    // SƠ ĐỒ GHẾ
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public List<SeatMapItemResponse> getSeatMap(UUID showtimeId) {
        return seatStatusRepository.findAllByShowtimeId(showtimeId).stream()
                .map(bookingMapper::toSeatMapItemResponse)
                .toList();
    }

    // =========================================================================
    // DANH SÁCH BOOKING
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getMyBookings(Pageable pageable) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return bookingRepository.findByUserId(userId, pageable)
                .map(bookingMapper::toBookingResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getAllBookings(BookingStatus status, Pageable pageable) {
        return bookingRepository.findAllByStatus(status, pageable)
                .map(bookingMapper::toBookingResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(UUID id) {
        return bookingRepository.findWithDetailsById(id)
                .map(bookingMapper::toBookingResponse)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
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
        Ticket ticket = ticketRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new AppException(ErrorCode.TICKET_NOT_FOUND));

        if (ticket.getStatus() == TicketStatus.USED) {
            throw new AppException(ErrorCode.TICKET_ALREADY_USED);
        }
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new AppException(ErrorCode.TICKET_CANCELLED);
        }

        ticket.setStatus(TicketStatus.USED);
        ticket.setCheckInTime(LocalDateTime.now());
        ticketRepository.save(ticket);

        log.info("Ticket checked-in qrCode={}", qrCode);
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

    /**
     * Sinh QR Code dùng full UUID để đảm bảo unique tuyệt đối.
     * Format: TKT-{bookingIdPrefix}-{seatIdPrefix}-{randomFull}
     * Trong production nên ký bằng HMAC-SHA256 để chống giả mạo.
     */
    private String generateQrCode(UUID bookingId, UUID seatId) {
        return "TKT-"
                + bookingId.toString().replace("-", "").substring(0, 12).toUpperCase()
                + "-" + seatId.toString().replace("-", "").substring(0, 12).toUpperCase()
                + "-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}
