package com.cinema.booking.controller;

import com.cinema.booking.dto.request.PaymentEventSearchRequest;
import com.cinema.booking.dto.request.PaymentSearchRequest;
import com.cinema.booking.dto.response.ApiResponse;
import com.cinema.booking.dto.response.PaymentEventResponse;
import com.cinema.booking.dto.response.PaymentReconciliationIssueResponse;
import com.cinema.booking.dto.response.PaymentResponse;
import com.cinema.booking.enums.PaymentEventType;
import com.cinema.booking.enums.PaymentMethod;
import com.cinema.booking.enums.PaymentStatus;
import com.cinema.booking.service.PaymentEventService;
import com.cinema.booking.service.PaymentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentController {

    PaymentService paymentService;
    PaymentEventService paymentEventService;

    @Value("${app.frontend-url:http://localhost:5173}")
    @NonFinal
    String frontendUrl;

    @PostMapping("/initiate")
    @PreAuthorize("hasAuthority('PAYMENT_CREATE')")
    public ApiResponse<String> initiatePayment(
            @RequestParam UUID bookingId,
            @RequestParam PaymentMethod method,
            @RequestParam BigDecimal amount,
            jakarta.servlet.http.HttpServletRequest request) {
        return ApiResponse.<String>builder()
                .code(1000)
                .message("Payment URL generated")
                .result(paymentService.initiatePayment(bookingId, method, amount, request))
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
            @ModelAttribute PaymentSearchRequest request,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.<Page<PaymentResponse>>builder()
                .code(1000)
                .result(paymentService.getAllPayments(request, pageable))
                .build();
    }

    @GetMapping("/events")
    @PreAuthorize("hasAuthority('PAYMENT_VIEW_ALL')")
    public ApiResponse<Page<PaymentEventResponse>> getPaymentEvents(
            @ModelAttribute PaymentEventSearchRequest request,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.<Page<PaymentEventResponse>>builder()
                .code(1000)
                .result(paymentEventService.search(request, pageable))
                .build();
    }

    @GetMapping("/reconciliation")
    @PreAuthorize("hasAuthority('PAYMENT_VIEW_ALL')")
    public ApiResponse<List<PaymentReconciliationIssueResponse>> getReconciliationIssues(
            @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.<List<PaymentReconciliationIssueResponse>>builder()
                .code(1000)
                .result(paymentService.getReconciliationIssues(limit))
                .build();
    }

    @GetMapping("/vnpay-callback")
    public void vnpayCallback(
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        String result = paymentService.handleVNPayCallback(request);
        
        if (result.startsWith("redirect:")) {
            String path = result.substring("redirect:".length());
            response.sendRedirect(normalizedFrontendUrl() + path);
        } else {
            response.sendRedirect(normalizedFrontendUrl() + "/payment/result?status=FAILED&reason=invalid_callback");
        }
    }

    @GetMapping("/momo-return")
    public void momoReturn(
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        String result = paymentService.handleMomoReturn(request);

        if (result.startsWith("redirect:")) {
            String path = result.substring("redirect:".length());
            response.sendRedirect(normalizedFrontendUrl() + path);
        } else {
            response.sendRedirect(normalizedFrontendUrl() + "/payment/result?status=FAILED&reason=invalid_callback");
        }
    }

    @PostMapping("/momo-ipn")
    public Map<String, Object> momoIpn(@RequestBody(required = false) Map<String, Object> payload) {
        return paymentService.handleMomoIpn(payload);
    }

    @PostMapping("/sepay-webhook")
    public Map<String, Object> sePayWebhook(
            @RequestBody(required = false) String rawPayload,
            jakarta.servlet.http.HttpServletRequest request) {
        return paymentService.handleSePayWebhook(rawPayload, request);
    }

    private String normalizedFrontendUrl() {
        return frontendUrl == null || frontendUrl.isBlank()
                ? "http://localhost:5173"
                : frontendUrl.replaceAll("/+$", "");
    }
}
