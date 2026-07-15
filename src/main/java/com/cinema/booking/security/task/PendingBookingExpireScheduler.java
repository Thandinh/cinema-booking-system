package com.cinema.booking.security.task;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.Payment;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.enums.PaymentStatus;
import com.cinema.booking.enums.SeatStatusType;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.PaymentRepository;
import com.cinema.booking.repository.SeatStatusRepository;
import com.cinema.booking.websocket.SeatStatusPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PendingBookingExpireScheduler {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final SeatStatusRepository seatStatusRepository;
    private final SeatStatusPublisher seatStatusPublisher;

    @Value("${booking.pending-timeout-minutes:15}")
    int pendingTimeoutMinutes;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expirePendingBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(pendingTimeoutMinutes);
        List<Booking> expiredBookings = bookingRepository.findExpiredPendingBookings(BookingStatus.PENDING, cutoff);

        if (expiredBookings.isEmpty()) {
            return;
        }

        for (Booking booking : expiredBookings) {
            List<UUID> seatIds = booking.getBookingDetails().stream()
                    .map(detail -> detail.getSeat().getId())
                    .toList();

            if (!seatIds.isEmpty()) {
                seatStatusRepository.bulkUpdateStatusAndClearHold(
                        booking.getShowtime().getId(),
                        seatIds,
                        SeatStatusType.AVAILABLE);
                seatStatusPublisher.publishBulk(booking.getShowtime().getId(), seatIds, SeatStatusType.AVAILABLE);
            }

            booking.setStatus(BookingStatus.FAILED);
        }

        List<UUID> bookingIds = expiredBookings.stream()
                .map(Booking::getId)
                .toList();
        List<Payment> pendingPayments = paymentRepository.findByBookingIdInAndStatus(
                bookingIds, PaymentStatus.PENDING);
        pendingPayments.forEach(payment -> payment.setStatus(PaymentStatus.FAILED));

        bookingRepository.saveAll(expiredBookings);
        paymentRepository.saveAll(pendingPayments);
        log.info("Expired {} pending bookings after {} minutes",
                expiredBookings.size(), pendingTimeoutMinutes);
    }
}
