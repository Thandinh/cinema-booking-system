package com.cinema.booking.controller;

import com.cinema.booking.dto.request.TicketCheckInRequest;
import com.cinema.booking.dto.response.ApiResponse;
import com.cinema.booking.dto.response.ShowtimeResponse;
import com.cinema.booking.dto.response.TicketResponse;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.service.BookingService;
import com.cinema.booking.service.ShowtimeService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TicketController {

    BookingService bookingService;
    ShowtimeService showtimeService;

    /** USER: Xem danh sách vé của chính mình */
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('TICKET_VIEW_OWN')")
    public ApiResponse<Page<TicketResponse>> getMyTickets(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.<Page<TicketResponse>>builder()
                .code(1000)
                .result(bookingService.getMyTickets(pageable))
                .build();
    }

    /** STAFF: Quét mã QR check-in vào rạp */
    @PostMapping("/check-in")
    @PreAuthorize("hasAuthority('TICKET_CHECKIN')")
    public ApiResponse<TicketResponse> checkInTicket(
            @RequestParam(required = false) String qrCode,
            @RequestParam(required = false) UUID cinemaId,
            @RequestParam(required = false) UUID showtimeId,
            @Valid @RequestBody(required = false) TicketCheckInRequest request) {
        return ApiResponse.<TicketResponse>builder()
                .code(1000)
                .result(bookingService.checkInTicket(
                        resolveQrCode(qrCode, request),
                        resolveCinemaId(cinemaId, request),
                        resolveShowtimeId(showtimeId, request)))
                .build();
    }

    /** ADMIN/STAFF: xem toàn bộ vé với phân trang */
    @GetMapping("/check-in/showtimes")
    @PreAuthorize("hasAuthority('TICKET_CHECKIN')")
    public ApiResponse<List<ShowtimeResponse>> getOpenCheckInShowtimes(@RequestParam UUID cinemaId) {
        return ApiResponse.<List<ShowtimeResponse>>builder()
                .code(1000)
                .result(showtimeService.getOpenCheckInShowtimes(cinemaId))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TICKET_VIEW_ALL')")
    public ApiResponse<Page<TicketResponse>> getAllTickets(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.<Page<TicketResponse>>builder()
                .code(1000)
                .result(bookingService.getAllTickets(pageable))
                .build();
    }

    private String resolveQrCode(String qrCode, TicketCheckInRequest request) {
        String value = request != null ? request.getQrCode() : qrCode;
        if (value == null || value.isBlank()) {
            throw new AppException(ErrorCode.TICKET_QR_REQUIRED);
        }
        return value;
    }

    private UUID resolveCinemaId(UUID cinemaId, TicketCheckInRequest request) {
        UUID value = request != null ? request.getCinemaId() : cinemaId;
        if (value == null) {
            throw new AppException(ErrorCode.TICKET_CHECKIN_CONTEXT_REQUIRED);
        }
        return value;
    }

    private UUID resolveShowtimeId(UUID showtimeId, TicketCheckInRequest request) {
        UUID value = request != null ? request.getShowtimeId() : showtimeId;
        if (value == null) {
            throw new AppException(ErrorCode.TICKET_CHECKIN_CONTEXT_REQUIRED);
        }
        return value;
    }
}
