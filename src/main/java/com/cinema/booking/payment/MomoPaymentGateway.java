package com.cinema.booking.payment;

import com.cinema.booking.configuration.MomoConfig;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.Payment;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.enums.PaymentMethod;
import com.cinema.booking.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MomoPaymentGateway implements PaymentGateway {

    MomoConfig momoConfig;
    RestTemplate momoRestTemplate;

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.MOMO;
    }

    @Override
    public String createPaymentUrl(Payment payment, Booking booking, HttpServletRequest request) {
        if (!momoConfig.isReady()) {
            throw new AppException(ErrorCode.PAYMENT_METHOD_UNAVAILABLE);
        }

        String cachedPayUrl = cachedPayUrl(payment);
        if (!cachedPayUrl.isBlank()) {
            return cachedPayUrl;
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
        payload.put("signature", signCreateRequest(amount, extraData, payment.getTransactionNo(), orderInfo));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map<String, Object>> response;
        try {
            response = momoRestTemplate.exchange(
                    momoConfig.getEndpoint(),
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    new ParameterizedTypeReference<>() {});
        } catch (RuntimeException ex) {
            log.error("MoMo create payment request failed for transaction {}", payment.getTransactionNo(), ex);
            throw new AppException(ErrorCode.PAYMENT_PROVIDER_ERROR);
        }

        Map<String, Object> body = response.getBody() == null ? Map.of() : response.getBody();
        payment.setProviderResponse(new LinkedHashMap<>(body));

        int resultCode = intValue(body.get("resultCode"));
        String payUrl = stringValue(body, "payUrl");
        if (!response.getStatusCode().is2xxSuccessful() || resultCode != 0 || payUrl.isBlank()) {
            log.warn("MoMo returned non-success create response for transaction {}: {}", payment.getTransactionNo(), body);
            throw new AppException(ErrorCode.PAYMENT_PROVIDER_ERROR);
        }

        return payUrl;
    }

    private String cachedPayUrl(Payment payment) {
        if (payment.getProviderResponse() == null) {
            return "";
        }
        return stringValue(payment.getProviderResponse(), "payUrl");
    }

    private String signCreateRequest(long amount, String extraData, String orderId, String orderInfo) {
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

    private String stringValue(Map<String, Object> map, String key) {
        if (map == null) {
            return "";
        }
        Object value = map.get(key);
        return value == null ? "" : value.toString();
    }
}
