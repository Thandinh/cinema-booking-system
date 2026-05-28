package com.cinema.booking.controller;

import com.cinema.booking.dto.response.ApiResponse;
import com.cinema.booking.dto.response.PaymentResponse;
import com.cinema.booking.enums.PaymentMethod;
import com.cinema.booking.service.PaymentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentController {

    PaymentService paymentService;

    @PostMapping("/initiate")
    @PreAuthorize("hasAuthority('PAYMENT_CREATE')")
    public ApiResponse<String> initiatePayment(
            @RequestParam UUID bookingId,
            @RequestParam PaymentMethod method,
            @RequestParam BigDecimal amount) {
        return ApiResponse.<String>builder()
                .code(1000)
                .message("Payment URL generated")
                .result(paymentService.initiatePayment(bookingId, method, amount))
                .build();
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('PAYMENT_VIEW_OWN')")
    public ApiResponse<Page<PaymentResponse>> getMyPayments(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ApiResponse.<Page<PaymentResponse>>builder()
                .code(1000)
                .result(paymentService.getMyPayments(pageable))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PAYMENT_VIEW_ALL')")
    public ApiResponse<Page<PaymentResponse>> getAllPayments(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.<Page<PaymentResponse>>builder()
                .code(1000)
                .result(paymentService.getAllPayments(pageable))
                .build();
    }
}
