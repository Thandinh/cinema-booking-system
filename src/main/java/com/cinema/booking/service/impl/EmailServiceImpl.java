package com.cinema.booking.service.impl;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.format.DateTimeFormatter;
import java.util.List;
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

    @Value("${spring.mail.username:noreply@cinema.com}")
    String senderEmail;

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
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("User {} has no email. Skipping ticket email for booking {}.",
                    booking.getUser().getUsername(), bookingId);
            return;
        }

        try {
            log.info("[Async] Sending ticket email to {} for booking {}", recipientEmail, bookingId);

            // Chuẩn bị dữ liệu truyền vào template
            String userName = (booking.getUser().getFirstName() != null ? booking.getUser().getFirstName() : "")
                    + " " + (booking.getUser().getLastName() != null ? booking.getUser().getLastName() : "");

            String seats = booking.getBookingDetails().stream()
                    .map(bd -> bd.getSeat().getRowLabel() + bd.getSeat().getSeatNumber())
                    .collect(Collectors.joining(", "));

            List<String> qrCodes = booking.getBookingDetails().stream()
                    .filter(bd -> bd.getTicket() != null)
                    .map(bd -> bd.getTicket().getQrCode())
                    .toList();

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

            // Build MIME message
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(recipientEmail);
            helper.setSubject("🎟️ Vé xem phim của bạn — " + booking.getShowtime().getMovie().getTitle());
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
            log.info("[Async] Ticket email sent successfully to {}", recipientEmail);

        } catch (MessagingException e) {
            // Lỗi SMTP không được để crash toàn bộ luồng thanh toán
            log.error("[Async] Failed to send ticket email for booking {}: {}", bookingId, e.getMessage());
        } catch (Exception e) {
            log.error("[Async] Unexpected error while sending email for booking {}: {}",
                    bookingId, e.getMessage());
        }
    }
}
