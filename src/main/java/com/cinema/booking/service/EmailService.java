package com.cinema.booking.service;

import java.util.UUID;

public interface EmailService {
    /**
     * Gửi email xác nhận vé sau khi thanh toán thành công.
     * Nhận UUID thay vì entity để tránh LazyInitializationException
     * khi chạy trong luồng @Async (Hibernate session đã đóng).
     */
    void sendTicketEmail(UUID bookingId);

    void sendShowtimeCancellationEmail(UUID bookingId, String reason);

    void sendEmailVerification(String recipientEmail, String username, String rawToken);

    void sendPasswordResetEmail(String recipientEmail, String username, String rawToken, long expiresMinutes);
}
