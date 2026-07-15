package com.cinema.booking.controller;

import com.cinema.booking.dto.request.CreateBookingRequest;
import com.cinema.booking.dto.request.HoldSeatRequest;
import com.cinema.booking.dto.request.TicketCheckInRequest;
import com.cinema.booking.dto.response.*;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingController {

    BookingService bookingService;

    // ── SEAT MAP ─────────────────────────────────────────────────────────────
    @GetMapping("/showtimes/{showtimeId}/seats")
    public ApiResponse<List<SeatMapItemResponse>> getSeatMap(@PathVariable UUID showtimeId) {
        return ApiResponse.<List<SeatMapItemResponse>>builder()
                .code(1000)
                .result(bookingService.getSeatMap(showtimeId))
                .build();
    }

    // ── HOLD SEATS ───────────────────────────────────────────────────────────
    @PostMapping("/hold")
    @PreAuthorize("hasAuthority('BOOKING_CREATE')")
    public ApiResponse<HoldSeatResponse> holdSeats(@Valid @RequestBody HoldSeatRequest request) {
        return ApiResponse.<HoldSeatResponse>builder()
                .code(1000)
                .message("Seats held successfully")
                .result(bookingService.holdSeats(request))
                .build();
    }

    // ── CREATE BOOKING ───────────────────────────────────────────────────────
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('BOOKING_CREATE')")
    public ApiResponse<BookingResponse> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        return ApiResponse.<BookingResponse>builder()
                .code(1000)
                .message("Booking created. Proceed to payment.")
                .result(bookingService.createBooking(request))
                .build();
    }

    // ── MY BOOKINGS ──────────────────────────────────────────────────────────
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('BOOKING_VIEW_OWN')")
    public ApiResponse<Page<BookingResponse>> getMyBookings(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ApiResponse.<Page<BookingResponse>>builder()
                .code(1000)
                .result(bookingService.getMyBookings(pageable))
                .build();
    }

    // ── ALL BOOKINGS (ADMIN/STAFF) ────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasAuthority('BOOKING_VIEW_ALL')")
    public ApiResponse<Page<BookingResponse>> getAllBookings(
            @RequestParam(required = false) BookingStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.<Page<BookingResponse>>builder()
                .code(1000)
                .result(bookingService.getAllBookings(status, pageable))
                .build();
    }

    // ── GET BY ID ────────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('BOOKING_VIEW_OWN','BOOKING_VIEW_ALL')")
    public ApiResponse<BookingResponse> getBookingById(@PathVariable UUID id) {
        return ApiResponse.<BookingResponse>builder()
                .code(1000)
                .result(bookingService.getBookingById(id))
                .build();
    }

    // ── CANCEL ───────────────────────────────────────────────────────────────
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('BOOKING_CANCEL_OWN','BOOKING_CANCEL_ALL')")
    public ApiResponse<BookingResponse> cancelBooking(@PathVariable UUID id) {
        return ApiResponse.<BookingResponse>builder()
                .code(1000)
                .message("Booking cancelled")
                .result(bookingService.cancelBooking(id))
                .build();
    }

    // ── TICKETS ──────────────────────────────────────────────────────────────
    @GetMapping("/tickets/my")
    @PreAuthorize("hasAuthority('TICKET_VIEW_OWN')")
    public ApiResponse<Page<TicketResponse>> getMyTickets(
            @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.<Page<TicketResponse>>builder()
                .code(1000)
                .result(bookingService.getMyTickets(pageable))
                .build();
    }

    @PostMapping("/tickets/check-in")
    @PreAuthorize("hasAuthority('TICKET_CHECKIN')")
    public ApiResponse<TicketResponse> checkIn(
            @RequestParam(required = false) String qrCode,
            @Valid @RequestBody(required = false) TicketCheckInRequest request) {
        return ApiResponse.<TicketResponse>builder()
                .code(1000)
                .message("Check-in successful")
                .result(bookingService.checkInTicket(resolveQrCode(qrCode, request)))
                .build();
    }

    private String resolveQrCode(String qrCode, TicketCheckInRequest request) {
        String value = request != null ? request.getQrCode() : qrCode;
        if (value == null || value.isBlank()) {
            throw new AppException(ErrorCode.TICKET_QR_REQUIRED);
        }
        return value;
    }
}
