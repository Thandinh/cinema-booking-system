package com.cinema.booking.payment;

import com.cinema.booking.configuration.VNPayConfig;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.Payment;
import com.cinema.booking.enums.PaymentMethod;
import com.cinema.booking.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VnPayPaymentGateway implements PaymentGateway {

    private static final ZoneId VNPAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    VNPayConfig vnpayConfig;

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.VNPAY;
    }

    @Override
    public String createPaymentUrl(Payment payment, Booking booking, HttpServletRequest request) {
        Map<String, String> vnpParamsMap = vnpayConfig.getVNPayConfig();
        vnpParamsMap.put("vnp_TxnRef", payment.getTransactionNo());
        vnpParamsMap.put("vnp_OrderInfo", "Thanh toan ve xem phim|" + booking.getSecureToken());
        vnpParamsMap.put("vnp_OrderType", "250000");
        vnpParamsMap.put("vnp_Amount", String.valueOf(payment.getAmount().multiply(new BigDecimal(100)).longValue()));
        vnpParamsMap.put("vnp_IpAddr", VNPayUtil.getIpAddress(request));

        LocalDateTime createDate = LocalDateTime.now(VNPAY_ZONE);
        LocalDateTime expireDate = booking.getPaymentExpiresAt() != null
                ? booking.getPaymentExpiresAt()
                : createDate.plusMinutes(15);
        vnpParamsMap.put("vnp_CreateDate", VNPAY_DATE_FORMATTER.format(createDate));
        vnpParamsMap.put("vnp_ExpireDate", VNPAY_DATE_FORMATTER.format(expireDate));

        List<String> fieldNames = new ArrayList<>(vnpParamsMap.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnpParamsMap.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String vnpSecureHash = VNPayUtil.hmacSHA512(vnpayConfig.getHashSecret().trim(), hashData.toString());
        return vnpayConfig.getUrl() + "?" + query + "&vnp_SecureHash=" + vnpSecureHash;
    }
}
