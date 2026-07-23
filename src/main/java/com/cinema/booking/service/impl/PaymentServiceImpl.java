package com.cinema.booking.service.impl;

import com.cinema.booking.configuration.MomoConfig;
import com.cinema.booking.configuration.VNPayConfig;
import com.cinema.booking.dto.response.PaymentReconciliationIssueResponse;
import com.cinema.booking.dto.response.PaymentResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.Payment;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.enums.PaymentEventType;
import com.cinema.booking.enums.PaymentMethod;
import com.cinema.booking.enums.PaymentStatus;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.mapper.PaymentMapper;
import com.cinema.booking.payment.PaymentGateway;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.PaymentRepository;
import com.cinema.booking.repository.PaymentReconciliationIssueRow;
import com.cinema.booking.service.PaymentEventService;
import com.cinema.booking.service.PaymentService;
import com.cinema.booking.util.SecurityUtils;
import com.cinema.booking.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    PaymentRepository paymentRepository;
    BookingRepository bookingRepository;
    VNPayConfig vnpayConfig;
    MomoConfig momoConfig;
    com.cinema.booking.service.BookingService bookingService;
    PaymentEventService paymentEventService;
    PaymentMapper paymentMapper;
    List<PaymentGateway> paymentGateways;

    @Override
    @Transactional
    public String initiatePayment(UUID bookingId, PaymentMethod method, BigDecimal amount, HttpServletRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new AppException(ErrorCode.BOOKING_ALREADY_PROCESSED);
        }

        if (isPaymentWindowExpired(booking)) {
            bookingService.expirePendingBooking(booking.getId());
            throw new AppException(ErrorCode.BOOKING_EXPIRED);
        }

        // Tạo record Payment PENDING
        if (amount == null || amount.compareTo(booking.getTotalPrice()) != 0) {
            throw new AppException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        Optional<Payment> pendingPayment = paymentRepository
                .findFirstByBookingIdAndMethodAndStatusOrderByCreatedAtDesc(
                        bookingId,
                        method,
                        PaymentStatus.PENDING);
        if (pendingPayment.isPresent()) {
            Payment payment = pendingPayment.get();
            log.info("Reusing pending payment {} for booking {}", payment.getTransactionNo(), bookingId);
            paymentEventService.record(
                    payment,
                    booking,
                    PaymentEventType.PAYMENT_REUSED,
                    payment.getStatus(),
                    payment.getStatus(),
                    booking.getStatus(),
                    booking.getStatus(),
                    true,
                    "Reused pending payment for booking",
                    null);
            return createPaymentUrlWithAudit(payment, booking, request);
        }

        String txnNo = generateTransactionNo(method, bookingId);
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(amount)
                .method(method)
                .transactionNo(txnNo)
                .status(PaymentStatus.PENDING)
                .build();
        
        paymentRepository.save(payment);
        log.info("Initiated payment {} for booking {}", txnNo, bookingId);
        paymentEventService.record(
                payment,
                booking,
                PaymentEventType.PAYMENT_INITIATED,
                null,
                payment.getStatus(),
                booking.getStatus(),
                booking.getStatus(),
                true,
                "Payment initiated",
                null);

        return createPaymentUrlWithAudit(payment, booking, request);
    }


    private String createPaymentUrlWithAudit(Payment payment, Booking booking, HttpServletRequest request) {
        try {
            String paymentUrl = gatewayFor(payment.getMethod()).createPaymentUrl(payment, booking, request);
            paymentRepository.save(payment);
            paymentEventService.record(
                    payment,
                    booking,
                    PaymentEventType.PAYMENT_URL_CREATED,
                    payment.getStatus(),
                    payment.getStatus(),
                    booking.getStatus(),
                    booking.getStatus(),
                    true,
                    "Payment URL created",
                    null);
            return paymentUrl;
        } catch (AppException ex) {
            paymentEventService.record(
                    payment,
                    booking,
                    PaymentEventType.PAYMENT_PROVIDER_ERROR,
                    payment.getStatus(),
                    payment.getStatus(),
                    booking.getStatus(),
                    booking.getStatus(),
                    false,
                    ex.getErrorCode().getMessage(),
                    null);
            throw ex;
        } catch (RuntimeException ex) {
            paymentEventService.record(
                    payment,
                    booking,
                    PaymentEventType.PAYMENT_PROVIDER_ERROR,
                    payment.getStatus(),
                    payment.getStatus(),
                    booking.getStatus(),
                    booking.getStatus(),
                    false,
                    ex.getMessage(),
                    null);
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getMyPayments(Pageable pageable) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return paymentRepository.findByUserId(userId, pageable)
                .map(paymentMapper::toPaymentResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(
            Pageable pageable,
            PaymentStatus status,
            PaymentMethod method,
            String keyword) {
        String keywordPattern = keyword == null || keyword.isBlank()
                ? null
                : "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        return paymentRepository.findAllWithDetails(status, method, keywordPattern, pageable)
                .map(paymentMapper::toPaymentResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentReconciliationIssueResponse> getReconciliationIssues(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return paymentRepository.findReconciliationIssues(LocalDateTime.now(), safeLimit).stream()
                .map(this::toReconciliationIssueResponse)
                .toList();
    }

    @Override
    @Transactional
    public String handleVNPayCallback(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0) && fieldName.startsWith("vnp_")) {
                try {
                    fields.put(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()), 
                               URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (Exception e) {
                    log.error("Error encoding VNPay param", e);
                }
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        if (fields.containsKey("vnp_SecureHashType")) {
            fields.remove("vnp_SecureHashType");
        }
        if (fields.containsKey("vnp_SecureHash")) {
            fields.remove("vnp_SecureHash");
        }

        // Verify checksum
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(fieldValue);
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }
        
        // Remove trailing spaces from secret key just in case
        String hashSecret = vnpayConfig.getHashSecret().trim();
        String signValue = VNPayUtil.hmacSHA512(hashSecret, hashData.toString());
        if (!signValue.equals(vnp_SecureHash)) {
            log.error("Invalid VNPay signature");
            paymentEventService.recordDetached(
                    null,
                    null,
                    PaymentMethod.VNPAY,
                    request.getParameter("vnp_TxnRef"),
                    PaymentEventType.VNPAY_CALLBACK_INVALID_SIGNATURE,
                    null,
                    null,
                    null,
                    null,
                    false,
                    "Invalid VNPay callback signature",
                    requestParamsToMap(request));
            return "redirect:/payment-failed?reason=invalid-signature";
        }

        String txnRef = request.getParameter("vnp_TxnRef");
        String responseCode = request.getParameter("vnp_ResponseCode");
        String orderInfo = request.getParameter("vnp_OrderInfo");
        
        // Extract secureToken from orderInfo (format: Thanh toan ve xem phim|SECURE_TOKEN)
        String[] parts = orderInfo.split("\\|");
        String secureToken = parts[parts.length - 1];

        Payment payment = paymentRepository.findLockedByTransactionNo(txnRef)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        Booking booking = payment.getBooking();
        appendProviderResponse(payment, "vnpayCallback", requestParamsToMap(request));
        recordPaymentEvent(
                payment,
                booking,
                PaymentEventType.VNPAY_CALLBACK_RECEIVED,
                true,
                "VNPay callback received",
                requestParamsToMap(request));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("Payment {} already processed", txnRef);
            paymentRepository.save(payment);
            recordPaymentEvent(
                    payment,
                    booking,
                    PaymentEventType.PAYMENT_ALREADY_PROCESSED,
                    true,
                    "VNPay callback ignored because payment was already processed",
                    requestParamsToMap(request));
            return "redirect:/payment/result?status=" + payment.getStatus()
                    + "&bookingId=" + booking.getId()
                    + "&txn=" + txnRef;
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            log.warn("Booking {} already processed before VNPay callback {}", booking.getId(), txnRef);
            payment.setStatus(booking.getStatus() == BookingStatus.SUCCESS
                    ? PaymentStatus.SUCCESS
                    : PaymentStatus.FAILED);
            if (payment.getStatus() == PaymentStatus.SUCCESS && payment.getPaymentTime() == null) {
                payment.setPaymentTime(LocalDateTime.now());
            }
            paymentRepository.save(payment);
            recordPaymentEvent(
                    payment,
                    booking,
                    PaymentEventType.PAYMENT_ALREADY_PROCESSED,
                    true,
                    "VNPay callback aligned payment with already finalized booking",
                    requestParamsToMap(request));
            return "redirect:/payment/result?status=" + payment.getStatus()
                    + "&bookingId=" + booking.getId()
                    + "&txn=" + txnRef;
        }

        if (!isValidVnpayAmount(request, payment)) {
            log.warn("VNPay amount mismatch for transaction {}", txnRef);
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            bookingService.handlePaymentFailure(secureToken);
            recordPaymentEvent(
                    payment,
                    booking,
                    PaymentEventType.VNPAY_AMOUNT_MISMATCH,
                    false,
                    "VNPay callback amount does not match payment amount",
                    requestParamsToMap(request));
            return "redirect:/payment/result?status=FAILED"
                    + "&bookingId=" + booking.getId()
                    + "&txn=" + txnRef;
        }

        if (isPaymentWindowExpired(booking)) {
            payment.setStatus(PaymentStatus.EXPIRED);
            paymentRepository.save(payment);
            bookingService.expirePendingBooking(booking.getId());
            recordPaymentEvent(
                    payment,
                    booking,
                    PaymentEventType.PAYMENT_EXPIRED,
                    false,
                    "VNPay callback arrived after payment window expired",
                    requestParamsToMap(request));
            return "redirect:/payment/result?status=EXPIRED"
                    + "&bookingId=" + booking.getId()
                    + "&txn=" + txnRef;
        }

        if ("00".equals(responseCode)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaymentTime(LocalDateTime.now());
            paymentRepository.save(payment);
            
            // Xử lý booking success (gửi email, đổi trạng thái vé...)
            bookingService.handlePaymentSuccess(secureToken);
            recordPaymentEvent(
                    payment,
                    booking,
                    PaymentEventType.PAYMENT_SUCCESS,
                    true,
                    "VNPay payment succeeded",
                    requestParamsToMap(request));
            return "redirect:/payment/result?status=SUCCESS"
                    + "&bookingId=" + booking.getId()
                    + "&txn=" + txnRef;
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            
            // Xử lý booking failed (nhả ghế...)
            bookingService.handlePaymentFailure(secureToken);
            recordPaymentEvent(
                    payment,
                    booking,
                    PaymentEventType.PAYMENT_FAILED,
                    false,
                    "VNPay payment failed",
                    requestParamsToMap(request));
            return "redirect:/payment/result?status=FAILED"
                    + "&bookingId=" + booking.getId()
                    + "&txn=" + txnRef;
        }
    }

    @Override
    @Transactional
    public String handleMomoReturn(HttpServletRequest request) {
        MomoProcessingResult result = processMomoCallback(requestParamsToMap(request));
        String bookingQuery = result.bookingId() == null ? "" : "&bookingId=" + result.bookingId();
        return "redirect:/payment/result?status=" + result.status()
                + bookingQuery
                + "&txn=" + urlEncode(result.transactionNo());
    }

    @Override
    @Transactional
    public Map<String, Object> handleMomoIpn(Map<String, Object> payload) {
        MomoProcessingResult result = processMomoCallback(payload == null ? Map.of() : payload);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("partnerCode", momoConfig.getPartnerCode());
        response.put("orderId", result.transactionNo());
        response.put("requestId", stringValue(payload, "requestId"));
        response.put("resultCode", result.resultCode());
        response.put("message", result.message());
        response.put("responseTime", System.currentTimeMillis());
        return response;
    }

    private MomoProcessingResult processMomoCallback(Map<String, Object> payload) {
        String orderId = stringValue(payload, "orderId");
        if (orderId.isBlank()) {
            paymentEventService.recordDetached(
                    null,
                    null,
                    PaymentMethod.MOMO,
                    "",
                    PaymentEventType.MOMO_CALLBACK_RECEIVED,
                    null,
                    null,
                    null,
                    null,
                    false,
                    "MoMo callback missing orderId",
                    payload);
            return new MomoProcessingResult("FAILED", null, "", 5, "Missing orderId");
        }

        Optional<Payment> optionalPayment = paymentRepository.findLockedByTransactionNo(orderId);
        if (optionalPayment.isEmpty()) {
            paymentEventService.recordDetached(
                    null,
                    null,
                    PaymentMethod.MOMO,
                    orderId,
                    PaymentEventType.MOMO_CALLBACK_RECEIVED,
                    null,
                    null,
                    null,
                    null,
                    false,
                    "MoMo callback payment not found",
                    payload);
            return new MomoProcessingResult("FAILED", null, orderId, 5, "Payment not found");
        }

        Payment payment = optionalPayment.get();
        Booking booking = payment.getBooking();
        UUID bookingId = booking.getId();

        if (!isValidMomoCallbackSignature(payload)) {
            log.warn("Invalid MoMo signature for transaction {}", orderId);
            appendProviderResponse(payment, "momoInvalidCallback", payload);
            paymentRepository.save(payment);
            recordPaymentEvent(
                    payment,
                    booking,
                    PaymentEventType.MOMO_CALLBACK_INVALID_SIGNATURE,
                    false,
                    "Invalid MoMo callback signature",
                    payload);
            return new MomoProcessingResult("FAILED", bookingId, orderId, 5, "Invalid signature");
        }

        long callbackAmount = longValue(payload.get("amount"));
        if (callbackAmount != toVndAmount(payment.getAmount())) {
            log.warn("MoMo amount mismatch for transaction {}. expected={}, actual={}",
                    orderId, payment.getAmount(), callbackAmount);
            appendProviderResponse(payment, "momoAmountMismatchCallback", payload);
            paymentRepository.save(payment);
            recordPaymentEvent(
                    payment,
                    booking,
                    PaymentEventType.MOMO_AMOUNT_MISMATCH,
                    false,
                    "MoMo callback amount does not match payment amount",
                    payload);
            return new MomoProcessingResult("FAILED", bookingId, orderId, 5, "Amount mismatch");
        }

        appendProviderResponse(payment, "momoCallback", payload);
        recordPaymentEvent(
                payment,
                booking,
                PaymentEventType.MOMO_CALLBACK_RECEIVED,
                true,
                "MoMo callback received",
                payload);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            paymentRepository.save(payment);
            recordPaymentEvent(
                    payment,
                    booking,
                    PaymentEventType.PAYMENT_ALREADY_PROCESSED,
                    true,
                    "MoMo callback ignored because payment was already processed",
                    payload);
            return new MomoProcessingResult(payment.getStatus().name(), bookingId, orderId, 0, "Already processed");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            payment.setStatus(booking.getStatus() == BookingStatus.SUCCESS
                    ? PaymentStatus.SUCCESS
                    : PaymentStatus.FAILED);
            if (payment.getStatus() == PaymentStatus.SUCCESS && payment.getPaymentTime() == null) {
                payment.setPaymentTime(LocalDateTime.now());
            }
            paymentRepository.save(payment);
            recordPaymentEvent(
                    payment,
                    booking,
                    PaymentEventType.PAYMENT_ALREADY_PROCESSED,
                    true,
                    "MoMo callback aligned payment with already finalized booking",
                    payload);
            return new MomoProcessingResult(payment.getStatus().name(), bookingId, orderId, 0, "Booking already processed");
        }

        if (isPaymentWindowExpired(booking)) {
            payment.setStatus(PaymentStatus.EXPIRED);
            paymentRepository.save(payment);
            bookingService.expirePendingBooking(bookingId);
            recordPaymentEvent(
                    payment,
                    booking,
                    PaymentEventType.PAYMENT_EXPIRED,
                    false,
                    "MoMo callback arrived after payment window expired",
                    payload);
            return new MomoProcessingResult("EXPIRED", bookingId, orderId, 0, "Booking expired");
        }

        int resultCode = intValue(payload.get("resultCode"));
        if (resultCode == 0) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaymentTime(LocalDateTime.now());
            paymentRepository.save(payment);
            bookingService.handlePaymentSuccess(booking.getSecureToken());
            recordPaymentEvent(
                    payment,
                    booking,
                    PaymentEventType.PAYMENT_SUCCESS,
                    true,
                    "MoMo payment succeeded",
                    payload);
            return new MomoProcessingResult("SUCCESS", bookingId, orderId, 0, "Success");
        }

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        if (booking.getStatus() == BookingStatus.PENDING) {
            bookingService.handlePaymentFailure(booking.getSecureToken());
        }
        recordPaymentEvent(
                payment,
                booking,
                PaymentEventType.PAYMENT_FAILED,
                false,
                "MoMo payment failed",
                payload);
        return new MomoProcessingResult("FAILED", bookingId, orderId, 0, "Payment failed");
    }

    private boolean isPaymentWindowExpired(Booking booking) {
        return booking.getPaymentExpiresAt() != null
                && !booking.getPaymentExpiresAt().isAfter(LocalDateTime.now());
    }

    private void recordPaymentEvent(
            Payment payment,
            Booking booking,
            PaymentEventType eventType,
            Boolean success,
            String message,
            Map<String, Object> payload) {
        paymentEventService.record(
                payment,
                booking,
                eventType,
                payment != null ? payment.getStatus() : null,
                payment != null ? payment.getStatus() : null,
                booking != null ? booking.getStatus() : null,
                booking != null ? booking.getStatus() : null,
                success,
                message,
                payload);
    }

    private boolean isValidVnpayAmount(HttpServletRequest request, Payment payment) {
        String rawAmount = request.getParameter("vnp_Amount");
        if (rawAmount == null || rawAmount.isBlank()) {
            return false;
        }
        try {
            long callbackAmount = Long.parseLong(rawAmount);
            return callbackAmount == toVndAmount(payment.getAmount()) * 100;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private String generateTransactionNo(PaymentMethod method, UUID bookingId) {
        if (method == PaymentMethod.MOMO) {
            return "MOMO_" + bookingId.toString().replace("-", "") + "_" + VNPayUtil.getRandomNumber(8);
        }
        return VNPayUtil.getRandomNumber(8);
    }

    private PaymentGateway gatewayFor(PaymentMethod method) {
        return paymentGateways.stream()
                .filter(gateway -> gateway.getMethod() == method)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_METHOD_UNAVAILABLE));
    }

    private boolean isValidMomoCallbackSignature(Map<String, Object> payload) {
        String signature = stringValue(payload, "signature");
        if (signature.isBlank() || !momoConfig.isReady()) {
            return false;
        }

        String rawData = "accessKey=" + momoConfig.getAccessKey()
                + "&amount=" + stringValue(payload, "amount")
                + "&extraData=" + stringValue(payload, "extraData")
                + "&message=" + stringValue(payload, "message")
                + "&orderId=" + stringValue(payload, "orderId")
                + "&orderInfo=" + stringValue(payload, "orderInfo")
                + "&orderType=" + stringValue(payload, "orderType")
                + "&partnerCode=" + stringValue(payload, "partnerCode")
                + "&payType=" + stringValue(payload, "payType")
                + "&requestId=" + stringValue(payload, "requestId")
                + "&responseTime=" + stringValue(payload, "responseTime")
                + "&resultCode=" + stringValue(payload, "resultCode")
                + "&transId=" + stringValue(payload, "transId");
        return hmacSha256(momoConfig.getSecretKey(), rawData).equalsIgnoreCase(signature);
    }

    private String hmacSha256(String secret, String rawData) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = hmac.doFinal(rawData.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new AppException(ErrorCode.PAYMENT_PROVIDER_ERROR);
        }
    }

    private Map<String, Object> requestParamsToMap(HttpServletRequest request) {
        Map<String, Object> params = new LinkedHashMap<>();
        for (Enumeration<String> names = request.getParameterNames(); names.hasMoreElements();) {
            String name = names.nextElement();
            params.put(name, request.getParameter(name));
        }
        return params;
    }

    private void appendProviderResponse(Payment payment, String key, Map<String, Object> value) {
        Map<String, Object> response = payment.getProviderResponse() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(payment.getProviderResponse());
        response.put(key, new LinkedHashMap<>(value));
        payment.setProviderResponse(response);
    }

    private long toVndAmount(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.UNNECESSARY).longValueExact();
    }

    private int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null || value.toString().isBlank()) {
            return -1;
        }
        return Integer.parseInt(value.toString());
    }

    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || value.toString().isBlank()) {
            return -1L;
        }
        return Long.parseLong(value.toString());
    }

    private String stringValue(Map<String, Object> map, String key) {
        if (map == null) {
            return "";
        }
        Object value = map.get(key);
        return value == null ? "" : value.toString();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private PaymentReconciliationIssueResponse toReconciliationIssueResponse(PaymentReconciliationIssueRow row) {
        return PaymentReconciliationIssueResponse.builder()
                .issueType(row.getIssueType())
                .severity(row.getSeverity())
                .bookingId(row.getBookingId())
                .paymentId(row.getPaymentId())
                .transactionNo(row.getTransactionNo())
                .bookingStatus(row.getBookingStatus())
                .paymentStatus(row.getPaymentStatus())
                .message(row.getMessage())
                .createdAt(row.getCreatedAt())
                .build();
    }

    private record MomoProcessingResult(
            String status,
            UUID bookingId,
            String transactionNo,
            int resultCode,
            String message
    ) {
    }
}
