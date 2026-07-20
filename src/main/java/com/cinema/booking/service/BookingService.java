package com.cinema.booking.service;

import com.cinema.booking.dto.request.CreateBookingRequest;
import com.cinema.booking.dto.request.HoldSeatRequest;
import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.dto.response.HoldSeatResponse;
import com.cinema.booking.dto.response.SeatMapItemResponse;
import com.cinema.booking.dto.response.TicketResponse;
import com.cinema.booking.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface BookingService {

    /** Bước 1: Giữ ghế (10 phút timeout). */
    HoldSeatResponse holdSeats(HoldSeatRequest request);

    /** Bước 2: Tạo booking PENDING + tính giá + áp voucher. */
    BookingResponse createBooking(CreateBookingRequest request);

    /** Callback: Thanh toán thành công → cập nhật booking, seat, tạo tickets. */
    BookingResponse handlePaymentSuccess(String secureToken);

    /** Callback: Thanh toán thất bại → nhả ghế về AVAILABLE. */
    BookingResponse handlePaymentFailure(String secureToken);

    /** Hủy booking (BOOKING_CANCEL_OWN / BOOKING_CANCEL_ALL). */
    BookingResponse cancelBooking(UUID bookingId);

    /** Hết hạn thanh toán, nhả ghế và đánh dấu EXPIRED. */
    BookingResponse expirePendingBooking(UUID bookingId);

    /** Xem sơ đồ ghế của suất chiếu. */
    List<SeatMapItemResponse> getSeatMap(UUID showtimeId);

    /** Xem booking của chính mình (BOOKING_VIEW_OWN). */
    Page<BookingResponse> getMyBookings(BookingStatus status, Pageable pageable);

    /** Xem tất cả booking (BOOKING_VIEW_ALL). */
    Page<BookingResponse> getAllBookings(BookingStatus status, Pageable pageable);

    /** Xem chi tiết 1 booking. */
    BookingResponse getBookingById(UUID id);

    /** Lấy vé của user (TICKET_VIEW_OWN). */
    Page<TicketResponse> getMyTickets(Pageable pageable);

    /** Check-in QR (TICKET_CHECKIN). */
    TicketResponse checkInTicket(String qrCode);
}
