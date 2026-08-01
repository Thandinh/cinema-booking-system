package com.cinema.booking.service;

import com.cinema.booking.dto.request.CreateBookingRequest;
import com.cinema.booking.dto.request.BookingSearchRequest;
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

    HoldSeatResponse holdSeats(HoldSeatRequest request);

    BookingResponse createBooking(CreateBookingRequest request);

    BookingResponse handlePaymentSuccess(String secureToken);

    BookingResponse handlePaymentFailure(String secureToken);

    BookingResponse cancelBooking(UUID bookingId);

    BookingResponse applyPromotion(UUID bookingId, String promotionCode);

    BookingResponse removePromotion(UUID bookingId);

    BookingResponse expirePendingBooking(UUID bookingId);

    List<SeatMapItemResponse> getSeatMap(UUID showtimeId);

    Page<BookingResponse> getMyBookings(BookingStatus status, Pageable pageable);

    Page<BookingResponse> getAllBookings(BookingSearchRequest request, Pageable pageable);

    BookingResponse getBookingById(UUID id);

    Page<TicketResponse> getMyTickets(Pageable pageable);

    Page<TicketResponse> getAllTickets(Pageable pageable);

    TicketResponse checkInTicket(String qrCode, UUID cinemaId, UUID showtimeId);
}
