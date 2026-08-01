package com.cinema.booking.payment;

import com.cinema.booking.configuration.SePayConfig;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.Payment;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.enums.PaymentMethod;
import com.cinema.booking.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SePayPaymentGateway implements PaymentGateway {

    private static final String RESPONSE_SCHEME = "sepay://pay?";

    SePayConfig sePayConfig;

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.SEPAY;
    }

    @Override
    public String createPaymentUrl(Payment payment, Booking booking, HttpServletRequest request) {
        if (!sePayConfig.isReady()) {
            throw new AppException(ErrorCode.PAYMENT_METHOD_UNAVAILABLE);
        }

        String cachedPayload = cachedPayload(payment);
        if (!cachedPayload.isBlank()) {
            return cachedPayload;
        }

        long amount = toVndAmount(payment.getAmount());
        String transferCode = payment.getTransactionNo();
        String transferContent = transferCode + " thanh toan ve";
        String qrUrl = buildQrUrl(amount, transferContent);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentType", "SEPAY_VIETQR");
        payload.put("bankCode", sePayConfig.getBankCode());
        payload.put("accountNumber", sePayConfig.getAccountNumber());
        payload.put("accountName", sePayConfig.getAccountName());
        payload.put("amount", amount);
        payload.put("transferCode", transferCode);
        payload.put("transferContent", transferContent);
        payload.put("qrUrl", qrUrl);
        payload.put("expiresAt", booking.getPaymentExpiresAt());

        Map<String, Object> providerResponse = payment.getProviderResponse() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(payment.getProviderResponse());
        providerResponse.put("sepayQr", payload);
        payment.setProviderResponse(providerResponse);

        return buildResponsePayload(payload);
    }

    private String cachedPayload(Payment payment) {
        if (payment.getProviderResponse() == null) {
            return "";
        }
        Object value = payment.getProviderResponse().get("sepayQr");
        if (!(value instanceof Map<?, ?> payload)) {
            return "";
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        payload.forEach((key, item) -> normalized.put(String.valueOf(key), item));
        String qrUrl = stringValue(normalized, "qrUrl");
        String transferCode = stringValue(normalized, "transferCode");
        long cachedAmount = longValue(normalized, "amount");
        long paymentAmount = toVndAmount(payment.getAmount());

        if (qrUrl.isBlank()
                || transferCode.isBlank()
                || !transferCode.equals(payment.getTransactionNo())
                || cachedAmount != paymentAmount) {
            return "";
        }
        return buildResponsePayload(normalized);
    }

    private String buildQrUrl(long amount, String transferContent) {
        return sePayConfig.getQrBaseUrl()
                + "?acc=" + urlEncode(sePayConfig.getAccountNumber())
                + "&bank=" + urlEncode(sePayConfig.getBankCode())
                + "&amount=" + amount
                + "&des=" + urlEncode(transferContent);
    }

    private String buildResponsePayload(Map<String, Object> payload) {
        return RESPONSE_SCHEME
                + "qrUrl=" + urlEncode(stringValue(payload, "qrUrl"))
                + "&bankCode=" + urlEncode(stringValue(payload, "bankCode"))
                + "&accountNumber=" + urlEncode(stringValue(payload, "accountNumber"))
                + "&accountName=" + urlEncode(stringValue(payload, "accountName"))
                + "&amount=" + urlEncode(stringValue(payload, "amount"))
                + "&transferCode=" + urlEncode(stringValue(payload, "transferCode"))
                + "&transferContent=" + urlEncode(stringValue(payload, "transferContent"))
                + "&expiresAt=" + urlEncode(stringValue(payload, "expiresAt"));
    }

    private long toVndAmount(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.UNNECESSARY).longValueExact();
    }

    private String stringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : value.toString();
    }

    private long longValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || value.toString().isBlank()) {
            return -1;
        }
        try {
            return new BigDecimal(value.toString()).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        } catch (ArithmeticException | NumberFormatException ex) {
            return -1;
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
