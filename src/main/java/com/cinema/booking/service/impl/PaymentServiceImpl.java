package com.cinema.booking.service.impl;

import com.cinema.booking.configuration.MomoConfig;
import com.cinema.booking.configuration.VNPayConfig;
import com.cinema.booking.dto.response.PaymentResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.Payment;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.enums.PaymentMethod;
import com.cinema.booking.enums.PaymentStatus;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.mapper.PaymentMapper;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.PaymentRepository;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    PaymentMapper paymentMapper;
    RestTemplate momoRestTemplate;

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

        return switch (method) {
            case VNPAY -> generateVNPayUrl(payment, booking, request);
            case MOMO -> generateMomoPayUrl(payment, booking);
            case CREDIT_CARD, CASH -> throw new AppException(ErrorCode.PAYMENT_METHOD_UNAVAILABLE);
        };
    }

    private String generateVNPayUrl(Payment payment, Booking booking, HttpServletRequest request) {
        Map<String, String> vnpParamsMap = vnpayConfig.getVNPayConfig();
        vnpParamsMap.put("vnp_TxnRef", payment.getTransactionNo());
        // Truyền secureToken vào OrderInfo để lúc callback có thể lấy ra xử lý booking (dùng dấu |)
        vnpParamsMap.put("vnp_OrderInfo", "Thanh toan ve xem phim|" + booking.getSecureToken());
        vnpParamsMap.put("vnp_OrderType", "250000"); // Mã danh mục giải trí
        vnpParamsMap.put("vnp_Amount", String.valueOf(payment.getAmount().multiply(new BigDecimal(100)).longValue()));
        vnpParamsMap.put("vnp_IpAddr", VNPayUtil.getIpAddress(request));
        
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(calendar.getTime());
        vnpParamsMap.put("vnp_CreateDate", vnpCreateDate);

        Date expireDate = booking.getPaymentExpiresAt() != null
                ? Date.from(booking.getPaymentExpiresAt().atZone(ZoneId.systemDefault()).toInstant())
                : new Date(System.currentTimeMillis() + 15 * 60 * 1000L);
        String vnp_ExpireDate = formatter.format(expireDate);
        vnpParamsMap.put("vnp_ExpireDate", vnp_ExpireDate);

        // Build hash data
        List<String> fieldNames = new ArrayList<>(vnpParamsMap.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnpParamsMap.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String hashSecret = vnpayConfig.getHashSecret().trim();
        String vnpSecureHash = VNPayUtil.hmacSHA512(hashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnpSecureHash;
        return vnpayConfig.getUrl() + "?" + queryUrl;
    }

    private String generateMomoPayUrl(Payment payment, Booking booking) {
        if (!momoConfig.isReady()) {
            throw new AppException(ErrorCode.PAYMENT_METHOD_UNAVAILABLE);
        }

        long amount = toVndAmount(payment.getAmount());
        String orderInfo = "Thanh toan ve xem phim " + booking.getSecureToken();
        String extraData = Base64.getEncoder().encodeToString(
                ("{\"bookingId\":\"" + booking.getId() + "\"}").getBytes(StandardCharsets.UTF_8));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("partnerCode", momoConfig.getPartnerCode());
        payload.put("requestType", momoConfig.getRequestType());
        payload.put("ipnUrl", momoConfig.getIpnUrl());
        payload.put("redirectUrl", momoConfig.getRedirectUrl());
        payload.put("orderId", payment.getTransactionNo());
        payload.put("amount", amount);
        payload.put("orderInfo", orderInfo);
        payload.put("requestId", payment.getTransactionNo());
        payload.put("extraData", extraData);
        payload.put("lang", momoConfig.getLang());
        payload.put("signature", signMomoCreateRequest(amount, extraData, payment.getTransactionNo(), orderInfo));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response;
        try {
            response = momoRestTemplate.postForEntity(
                    momoConfig.getEndpoint(),
                    new HttpEntity<>(payload, headers),
                    Map.class);
        } catch (RuntimeException ex) {
            log.error("MoMo create payment request failed for transaction {}", payment.getTransactionNo(), ex);
            throw new AppException(ErrorCode.PAYMENT_PROVIDER_ERROR);
        }

        Map<String, Object> body = response.getBody() == null ? Map.of() : response.getBody();
        payment.setProviderResponse(new LinkedHashMap<>(body));
        paymentRepository.save(payment);

        int resultCode = intValue(body.get("resultCode"));
        String payUrl = stringValue(body, "payUrl");
        if (!response.getStatusCode().is2xxSuccessful() || resultCode != 0 || payUrl.isBlank()) {
            log.warn("MoMo returned non-success create response for transaction {}: {}", payment.getTransactionNo(), body);
            throw new AppException(ErrorCode.PAYMENT_PROVIDER_ERROR);
        }

        return payUrl;
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
            return "redirect:/payment-failed?reason=invalid-signature";
        }

        String txnRef = request.getParameter("vnp_TxnRef");
        String responseCode = request.getParameter("vnp_ResponseCode");
        String orderInfo = request.getParameter("vnp_OrderInfo");
        
        // Extract secureToken from orderInfo (format: Thanh toan ve xem phim|SECURE_TOKEN)
        String[] parts = orderInfo.split("\\|");
        String secureToken = parts[parts.length - 1];

        Payment payment = paymentRepository.findByTransactionNo(txnRef)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("Payment {} already processed", txnRef);
            return "redirect:/payment/result?status=" + payment.getStatus()
                    + "&bookingId=" + payment.getBooking().getId()
                    + "&txn=" + txnRef;
        }

        if (isPaymentWindowExpired(payment.getBooking())) {
            payment.setStatus(PaymentStatus.EXPIRED);
            paymentRepository.save(payment);
            bookingService.expirePendingBooking(payment.getBooking().getId());
            return "redirect:/payment/result?status=EXPIRED"
                    + "&bookingId=" + payment.getBooking().getId()
                    + "&txn=" + txnRef;
        }

        if ("00".equals(responseCode)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaymentTime(LocalDateTime.now());
            paymentRepository.save(payment);
            
            // Xử lý booking success (gửi email, đổi trạng thái vé...)
            bookingService.handlePaymentSuccess(secureToken);
            return "redirect:/payment/result?status=SUCCESS"
                    + "&bookingId=" + payment.getBooking().getId()
                    + "&txn=" + txnRef;
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            
            // Xử lý booking failed (nhả ghế...)
            bookingService.handlePaymentFailure(secureToken);
            return "redirect:/payment/result?status=FAILED"
                    + "&bookingId=" + payment.getBooking().getId()
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
            return new MomoProcessingResult("FAILED", null, "", 5, "Missing orderId");
        }

        Optional<Payment> optionalPayment = paymentRepository.findLockedByTransactionNo(orderId);
        if (optionalPayment.isEmpty()) {
            return new MomoProcessingResult("FAILED", null, orderId, 5, "Payment not found");
        }

        Payment payment = optionalPayment.get();
        Booking booking = payment.getBooking();
        UUID bookingId = booking.getId();

        if (!isValidMomoCallbackSignature(payload)) {
            log.warn("Invalid MoMo signature for transaction {}", orderId);
            appendProviderResponse(payment, "momoInvalidCallback", payload);
            paymentRepository.save(payment);
            return new MomoProcessingResult("FAILED", bookingId, orderId, 5, "Invalid signature");
        }

        long callbackAmount = longValue(payload.get("amount"));
        if (callbackAmount != toVndAmount(payment.getAmount())) {
            log.warn("MoMo amount mismatch for transaction {}. expected={}, actual={}",
                    orderId, payment.getAmount(), callbackAmount);
            appendProviderResponse(payment, "momoAmountMismatchCallback", payload);
            paymentRepository.save(payment);
            return new MomoProcessingResult("FAILED", bookingId, orderId, 5, "Amount mismatch");
        }

        appendProviderResponse(payment, "momoCallback", payload);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            paymentRepository.save(payment);
            return new MomoProcessingResult(payment.getStatus().name(), bookingId, orderId, 0, "Already processed");
        }

        if (isPaymentWindowExpired(booking)) {
            payment.setStatus(PaymentStatus.EXPIRED);
            paymentRepository.save(payment);
            bookingService.expirePendingBooking(bookingId);
            return new MomoProcessingResult("EXPIRED", bookingId, orderId, 0, "Booking expired");
        }

        int resultCode = intValue(payload.get("resultCode"));
        if (resultCode == 0) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaymentTime(LocalDateTime.now());
            paymentRepository.save(payment);
            bookingService.handlePaymentSuccess(booking.getSecureToken());
            return new MomoProcessingResult("SUCCESS", bookingId, orderId, 0, "Success");
        }

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        if (booking.getStatus() == BookingStatus.PENDING) {
            bookingService.handlePaymentFailure(booking.getSecureToken());
        }
        return new MomoProcessingResult("FAILED", bookingId, orderId, 0, "Payment failed");
    }

    private boolean isPaymentWindowExpired(Booking booking) {
        return booking.getPaymentExpiresAt() != null
                && !booking.getPaymentExpiresAt().isAfter(LocalDateTime.now());
    }

    private String generateTransactionNo(PaymentMethod method, UUID bookingId) {
        if (method == PaymentMethod.MOMO) {
            return "MOMO_" + bookingId.toString().replace("-", "") + "_" + VNPayUtil.getRandomNumber(8);
        }
        return VNPayUtil.getRandomNumber(8);
    }

    private String signMomoCreateRequest(long amount, String extraData, String orderId, String orderInfo) {
        String rawData = "accessKey=" + momoConfig.getAccessKey()
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + momoConfig.getIpnUrl()
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + momoConfig.getPartnerCode()
                + "&redirectUrl=" + momoConfig.getRedirectUrl()
                + "&requestId=" + orderId
                + "&requestType=" + momoConfig.getRequestType();
        return hmacSha256(momoConfig.getSecretKey(), rawData);
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

    private record MomoProcessingResult(
            String status,
            UUID bookingId,
            String transactionNo,
            int resultCode,
            String message
    ) {
    }
}
