package com.cinema.booking.service;

import com.cinema.booking.dto.response.PaymentReconciliationIssueResponse;
import com.cinema.booking.dto.response.PaymentResponse;
import com.cinema.booking.enums.PaymentMethod;
import com.cinema.booking.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
    Page<PaymentResponse> getAllPayments(Pageable pageable, PaymentStatus status, PaymentMethod method, String keyword);

    List<PaymentReconciliationIssueResponse> getReconciliationIssues(int limit);

    /** Xử lý callback trả về từ VNPay */
    String handleVNPayCallback(HttpServletRequest request);

    /** Xử lý redirect trả về từ MoMo sau khi user hoàn tất/hủy thanh toán. */
    String handleMomoReturn(HttpServletRequest request);

    /** Xử lý IPN server-to-server từ MoMo. IPN là nguồn xác nhận đáng tin cậy nhất. */
    Map<String, Object> handleMomoIpn(Map<String, Object> payload);

    Map<String, Object> handleSePayWebhook(String rawPayload, HttpServletRequest request);
}
