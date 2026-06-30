package com.cinema.booking.service.impl;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    PaymentRepository paymentRepository;
    BookingRepository bookingRepository;
    VNPayConfig vnpayConfig;
    com.cinema.booking.service.BookingService bookingService;
    PaymentMapper paymentMapper;

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

        // Tạo record Payment PENDING
        String txnNo = VNPayUtil.getRandomNumber(8);
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(amount)
                .method(method)
                .transactionNo(txnNo)
                .status(PaymentStatus.PENDING)
                .build();
        
        paymentRepository.save(payment);
        log.info("Initiated payment {} for booking {}", txnNo, bookingId);

        if (method == PaymentMethod.VNPAY) {
            return generateVNPayUrl(payment, booking, request);
        }

        return "https://mock-payment-gateway.com/pay?txn=" + txnNo + "&token=" + booking.getSecureToken();
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

        calendar.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(calendar.getTime());
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

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getMyPayments(Pageable pageable) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return paymentRepository.findByUserId(userId, pageable)
                .map(paymentMapper::toPaymentResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(Pageable pageable) {
        return paymentRepository.findAllWithDetails(pageable)
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
            return "redirect:/payment-success?txn=" + txnRef;
        }

        if ("00".equals(responseCode)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaymentTime(LocalDateTime.now());
            paymentRepository.save(payment);
            
            // Xử lý booking success (gửi email, đổi trạng thái vé...)
            bookingService.handlePaymentSuccess(secureToken);
            return "redirect:/payment-success?txn=" + txnRef;
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            
            // Xử lý booking failed (nhả ghế...)
            bookingService.handlePaymentFailure(secureToken);
            return "redirect:/payment-failed?txn=" + txnRef;
        }
    }
}
