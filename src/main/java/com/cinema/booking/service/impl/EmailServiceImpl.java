package com.cinema.booking.service.impl;

import com.cinema.booking.entity.Booking;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
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

            List<Map<String, String>> qrCodes = new ArrayList<>();
            List<Map<String, String>> localQrCodes = new ArrayList<>();
            Map<String, byte[]> inlineQrImages = new LinkedHashMap<>();
            int qrIndex = 1;
            for (var detail : booking.getBookingDetails()) {
                if (detail.getTicket() == null) {
                    continue;
                }

                String qrCode = detail.getTicket().getQrCode();
                byte[] pngBytes = qrCodeImageService.toPngBytes(qrCode, 360);
                String contentId = "ticket-qr-" + qrIndex++;

                qrCodes.add(Map.of("code", qrCode, "image", "cid:" + contentId));
                localQrCodes.add(Map.of(
                        "code", qrCode,
                        "image", "data:image/png;base64," + Base64.getEncoder().encodeToString(pngBytes)));
                inlineQrImages.put(contentId, pngBytes);
            }

            // Thymeleaf context
            Context context = new Context();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
            context.setVariable("userName",    userName.trim());
            context.setVariable("movieTitle",  booking.getShowtime().getMovie().getTitle());
            context.setVariable("cinemaName",  booking.getShowtime().getRoom().getCinema().getName());
            context.setVariable("roomName",    booking.getShowtime().getRoom().getName());
            context.setVariable("showTime",    booking.getShowtime().getStartTime().format(fmt));
            context.setVariable("seats",       seats);
            context.setVariable("totalPrice",  booking.getTotalPrice());
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

    private boolean isMailConfigured() {
        return senderEmail != null
                && !senderEmail.isBlank()
                && !isPlaceholder(senderEmail)
                && !senderEmail.equalsIgnoreCase("noreply@cinema.com")
                && senderPassword != null
                && !senderPassword.isBlank()
                && !isPlaceholder(senderPassword);
    }

    private boolean isPlaceholder(String value) {
        String normalized = value.toLowerCase();
        return normalized.contains("your_")
                || normalized.contains("placeholder")
                || normalized.contains("change_me")
                || normalized.contains("example");
    }
}
