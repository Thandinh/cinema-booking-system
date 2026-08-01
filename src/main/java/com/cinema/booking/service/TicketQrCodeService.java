package com.cinema.booking.service;

import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.exception.AppException;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TicketQrCodeService {

    static final String VERSION = "CBT1";
    static final String HMAC_ALGORITHM = "HmacSHA256";
    static final int NONCE_BYTES = 16;
    static final int SIGNATURE_BYTES = 24;
    static final Pattern TOKEN_PATTERN = Pattern.compile(
            "^CBT1\\.[0-9A-F]{32}\\.[A-Za-z0-9_-]{22}\\.[A-Za-z0-9_-]{32}$");

    final SecureRandom secureRandom = new SecureRandom();
    final Base64.Encoder base64UrlEncoder = Base64.getUrlEncoder().withoutPadding();

    @Value("${ticket.qr-secret:${jwt.signer-key:default-dev-secret-key-32-chars-minimum}}")
    String qrSecret;

    SecretKeySpec signingKey;

    @PostConstruct
    void init() {
        if (qrSecret == null || qrSecret.length() < 32) {
            throw new IllegalStateException("ticket.qr-secret must be at least 32 characters");
        }
        signingKey = new SecretKeySpec(qrSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
    }

    public String generate(UUID bookingDetailId) {
        String nonce = randomNonce();
        String payload = VERSION + "." + compactUuid(bookingDetailId) + "." + nonce;
        return payload + "." + sign(payload);
    }

    public String normalizeAndValidate(String qrCode) {
        if (qrCode == null || qrCode.isBlank()) {
            throw new AppException(ErrorCode.TICKET_QR_REQUIRED);
        }

        String token = qrCode.trim();
        if (token.length() > 100 || !TOKEN_PATTERN.matcher(token).matches()) {
            throw new AppException(ErrorCode.INVALID_QR_CODE);
        }

        String[] parts = token.split("\\.");
        String payload = parts[0] + "." + parts[1] + "." + parts[2];
        String expectedSignature = sign(payload);

        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                parts[3].getBytes(StandardCharsets.UTF_8))) {
            throw new AppException(ErrorCode.INVALID_QR_CODE);
        }

        return token;
    }

    public boolean isValidSignedToken(String qrCode) {
        try {
            normalizeAndValidate(qrCode);
            return true;
        } catch (AppException e) {
            return false;
        }
    }

    private String randomNonce() {
        byte[] nonce = new byte[NONCE_BYTES];
        secureRandom.nextBytes(nonce);
        return base64UrlEncoder.encodeToString(nonce);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(signingKey);
            byte[] signature = Arrays.copyOf(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)), SIGNATURE_BYTES);
            return base64UrlEncoder.encodeToString(signature);
        } catch (Exception e) {
            throw new IllegalStateException("Could not sign ticket QR code", e);
        }
    }

    private String compactUuid(UUID value) {
        return value.toString().replace("-", "").toUpperCase();
    }
}
