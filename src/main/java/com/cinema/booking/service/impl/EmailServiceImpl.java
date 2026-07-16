package com.cinema.booking.service.impl;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.Cinema;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.service.EmailService;
import com.cinema.booking.service.QrCodeImageService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@Slf4j
public class EmailServiceImpl implements EmailService {

    final JavaMailSender     javaMailSender;
    final TemplateEngine     templateEngine;
    final BookingRepository  bookingRepository;
    final QrCodeImageService qrCodeImageService;

    @Value("${spring.mail.username:noreply@cinema.com}")
    String senderEmail;

    @Value("${spring.mail.password:}")
    String senderPassword;

    @Value("${app.frontend-url:http://localhost:5173}")
    String frontendUrl;

    /**
     * Chạy bất đồng bộ (@Async) trong thread pool riêng.
     * @Transactional mở một session JPA MỚI trong thread này,
     * cho phép lazy loading hoạt động bình thường.
     * Dùng findByIdForEmail() để JOIN FETCH toàn bộ quan hệ trong 1 query duy nhất.
     */
    @Async
    @Override
    @Transactional(readOnly = true)
    public void sendTicketEmail(UUID bookingId) {
        // Reload booking với tất cả quan hệ cần thiết (1 query, không N+1)
        Booking booking = bookingRepository.findByIdForEmail(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        String recipientEmail = booking.getUser().getEmail();

        try {
            log.info("[Async] Sending ticket email to {} for booking {}", recipientEmail, bookingId);

            // Chuẩn bị dữ liệu truyền vào template
            String userName = (booking.getUser().getFirstName() != null ? booking.getUser().getFirstName() : "")
                    + " " + (booking.getUser().getLastName() != null ? booking.getUser().getLastName() : "");

            String seats = booking.getBookingDetails().stream()
                    .map(bd -> bd.getSeat().getRowLabel() + bd.getSeat().getSeatNumber())
                    .collect(Collectors.joining(", "));
            Cinema cinema = booking.getShowtime().getRoom().getCinema();
            String cinemaAddress = buildCinemaAddress(cinema);

            List<Map<String, String>> qrCodes = new ArrayList<>();
            List<Map<String, String>> localQrCodes = new ArrayList<>();
            Map<String, byte[]> inlineQrImages = new LinkedHashMap<>();
            int qrIndex = 1;
            for (var detail : booking.getBookingDetails()) {
                if (detail.getTicket() == null) {
                    continue;
                }

                String qrCode = detail.getTicket().getQrCode();
                String seatLabel = detail.getSeat().getRowLabel() + detail.getSeat().getSeatNumber();
                byte[] pngBytes = qrCodeImageService.toPngBytes(qrCode, 360);
                String contentId = "ticket-qr-" + qrIndex++;

                qrCodes.add(Map.of("code", qrCode, "image", "cid:" + contentId, "seat", seatLabel));
                localQrCodes.add(Map.of(
                        "code", qrCode,
                        "seat", seatLabel,
                        "image", "data:image/png;base64," + Base64.getEncoder().encodeToString(pngBytes)));
                inlineQrImages.put(contentId, pngBytes);
            }

            if (qrCodes.isEmpty()) {
                log.warn("[Async] Booking {} has no ticket QR codes. Ticket email will show a support warning.", bookingId);
            }

            // Thymeleaf context
            Context context = new Context();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
            context.setVariable("userName",    userName.trim());
            context.setVariable("movieTitle",  booking.getShowtime().getMovie().getTitle());
            context.setVariable("cinemaName",  booking.getShowtime().getRoom().getCinema().getName());
            context.setVariable("cinemaAddress", cinemaAddress);
            context.setVariable("roomName",    booking.getShowtime().getRoom().getName());
            context.setVariable("showTime",    booking.getShowtime().getStartTime().format(fmt).replace(" - ", " · "));
            context.setVariable("seats",       seats);
            context.setVariable("totalPrice",  formatVnd(booking.getTotalPrice()));
            context.setVariable("qrCodes",     qrCodes);

            // Render HTML
            String htmlContent = templateEngine.process("ticket-email", context);
            saveLocalTicketEmail(bookingId, context, localQrCodes);

            if (recipientEmail == null || recipientEmail.isBlank()) {
                log.warn("User {} has no email. Local ticket email was saved for booking {}.",
                        booking.getUser().getUsername(), bookingId);
                return;
            }
            if (!isMailConfigured()) {
                log.warn("SMTP credentials are not configured. Local ticket email was saved for booking {}.", bookingId);
                return;
            }

            // Build MIME message
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(recipientEmail);
            helper.setSubject("🎟️ Vé xem phim của bạn — " + booking.getShowtime().getMovie().getTitle());
            helper.setText(htmlContent, true);
            for (Map.Entry<String, byte[]> entry : inlineQrImages.entrySet()) {
                helper.addInline(entry.getKey(), new ByteArrayResource(entry.getValue()), "image/png");
            }

            javaMailSender.send(message);
            log.info("[Async] Ticket email sent successfully to {}", recipientEmail);

        } catch (MailAuthenticationException e) {
            log.error("[Async] SMTP authentication failed for booking {}. Check MAIL_USERNAME/MAIL_PASSWORD or provider app password. Local ticket email was already saved.",
                    bookingId);
        } catch (MailException e) {
            log.error("[Async] SMTP send failed for booking {}: {}. Local ticket email was already saved.",
                    bookingId, e.getMessage());
        } catch (MessagingException e) {
            // Lỗi SMTP không được để crash toàn bộ luồng thanh toán
            log.error("[Async] Failed to send ticket email for booking {}: {}", bookingId, e.getMessage());
        } catch (Exception e) {
            log.error("[Async] Unexpected error while sending email for booking {}: {}",
                    bookingId, e.getMessage());
        }
    }

    @Async
    @Override
    public void sendEmailVerification(String recipientEmail, String username, String rawToken) {
        try {
            if (recipientEmail == null || recipientEmail.isBlank()) {
                log.warn("[Async] Skip verification email because recipient is blank for user {}", username);
                return;
            }

            String verificationUrl = frontendUrl.replaceAll("/+$", "")
                    + "/verify-email?token="
                    + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);

            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("verificationUrl", verificationUrl);
            context.setVariable("expiresIn", "24 giờ");

            String htmlContent = templateEngine.process("email-verification", context);
            saveLocalVerificationEmail(recipientEmail, context);

            if (!isMailConfigured()) {
                log.warn("SMTP credentials are not configured. Local verification email was saved for {}.", recipientEmail);
                return;
            }

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(recipientEmail);
            helper.setSubject("Xác thực tài khoản cinemabooking.vn");
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
            log.info("[Async] Verification email sent successfully to {}", recipientEmail);
        } catch (MailAuthenticationException e) {
            log.error("[Async] SMTP authentication failed while sending verification email to {}.", recipientEmail);
        } catch (MailException | MessagingException e) {
            log.error("[Async] Verification email send failed for {}: {}", recipientEmail, e.getMessage());
        } catch (Exception e) {
            log.error("[Async] Unexpected error while sending verification email to {}: {}", recipientEmail, e.getMessage());
        }
    }

    private void saveLocalTicketEmail(UUID bookingId, Context context, List<Map<String, String>> localQrCodes) {
        try {
            context.setVariable("qrCodes", localQrCodes);
            String localHtmlContent = templateEngine.process("ticket-email", context);
            Path outboxDir = Path.of("logs", "emails");
            Files.createDirectories(outboxDir);
            Path emailPath = outboxDir.resolve("ticket-" + bookingId + ".html");
            Files.writeString(emailPath, localHtmlContent);
            log.info("[Async] Local ticket email saved at {}", emailPath.toAbsolutePath());
        } catch (Exception e) {
            log.warn("[Async] Could not save local ticket email for booking {}: {}", bookingId, e.getMessage());
        }
    }

    private void saveLocalVerificationEmail(String recipientEmail, Context context) {
        try {
            String localHtmlContent = templateEngine.process("email-verification", context);
            Path outboxDir = Path.of("logs", "emails");
            Files.createDirectories(outboxDir);
            String safeEmail = recipientEmail.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path emailPath = outboxDir.resolve("verify-" + safeEmail + ".html");
            Files.writeString(emailPath, localHtmlContent);
            log.info("[Async] Local verification email saved at {}", emailPath.toAbsolutePath());
        } catch (Exception e) {
            log.warn("[Async] Could not save local verification email for {}: {}", recipientEmail, e.getMessage());
        }
    }

    private boolean isMailConfigured() {
        return senderEmail != null
                && !senderEmail.isBlank()
                && !isPlaceholder(senderEmail)
                && !senderEmail.equalsIgnoreCase("noreply@cinema.com")
                && senderPassword != null
                && !senderPassword.isBlank()
                && !isPlaceholder(senderPassword);
    }

    private String buildCinemaAddress(Cinema cinema) {
        if (cinema == null) return "";
        String address = cinema.getAddress() == null ? "" : cinema.getAddress().trim();
        String city = cinema.getCity() == null ? "" : cinema.getCity().trim();
        if (address.isBlank()) return city;
        if (city.isBlank() || address.toLowerCase().contains(city.toLowerCase())) return address;
        return address + ", " + city;
    }

    private String formatVnd(java.math.BigDecimal amount) {
        if (amount == null) return "0 đ";
        return java.text.NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
                .format(amount.setScale(0, java.math.RoundingMode.HALF_UP))
                + " đ";
    }

    private boolean isPlaceholder(String value) {
        String normalized = value.toLowerCase();
        return normalized.contains("your_")
                || normalized.contains("placeholder")
                || normalized.contains("change_me")
                || normalized.contains("example");
    }
}
