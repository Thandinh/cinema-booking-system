package com.cinema.booking.controller;

import com.cinema.booking.dto.request.TicketCheckInRequest;
import com.cinema.booking.dto.response.ApiResponse;
import com.cinema.booking.dto.response.TicketResponse;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.mapper.TicketMapper;
import com.cinema.booking.repository.TicketRepository;
import com.cinema.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TicketController {

    TicketRepository ticketRepository;
    TicketMapper ticketMapper;
    BookingService bookingService;

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
            @Valid @RequestBody(required = false) TicketCheckInRequest request) {
        return ApiResponse.<TicketResponse>builder()
                .code(1000)
                .result(bookingService.checkInTicket(resolveQrCode(qrCode, request)))
                .build();
    }

    /** ADMIN/STAFF: xem toàn bộ vé với phân trang */
    @GetMapping
    @PreAuthorize("hasAuthority('TICKET_VIEW_ALL')")
    public ApiResponse<Page<TicketResponse>> getAllTickets(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.<Page<TicketResponse>>builder()
                .code(1000)
                .result(ticketRepository.findAllWithDetails(pageable)
                        .map(ticketMapper::toTicketResponse))
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
