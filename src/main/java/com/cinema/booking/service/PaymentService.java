package com.cinema.booking.service;

import com.cinema.booking.dto.response.PaymentResponse;
import com.cinema.booking.enums.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;

public interface PaymentService {

    /**
     * Khởi tạo giao dịch thanh toán (VD: sinh URL VNPay)
     * Trả về URL để frontend redirect người dùng.
     */
    String initiatePayment(UUID bookingId, PaymentMethod method, BigDecimal amount, HttpServletRequest request);

    /** Lấy danh sách giao dịch của user (PAYMENT_VIEW_OWN) */
    Page<PaymentResponse> getMyPayments(Pageable pageable);

    /** Lấy toàn bộ giao dịch cho ADMIN/STAFF (PAYMENT_VIEW_ALL) */
    Page<PaymentResponse> getAllPayments(Pageable pageable);

    /** Xử lý callback trả về từ VNPay */
    String handleVNPayCallback(HttpServletRequest request);
}
