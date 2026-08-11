package com.cinema.booking.security.task;

import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "booking.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class PendingBookingExpireScheduler {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    @Value("${booking.pending-timeout-minutes:5}")
    int pendingTimeoutMinutes;

    @Value("${booking.expired-booking-scan-limit:200}")
    int expiredBookingScanLimit;

    @Scheduled(fixedDelayString = "${booking.expired-booking-scan-delay-ms:30000}")
    public void expirePendingBookings() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime legacyCutoff = now.minusMinutes(pendingTimeoutMinutes);

        List<UUID> expiredBookingIds = bookingRepository.findExpiredPendingBookingIds(
                BookingStatus.PENDING.name(),
                now,
                legacyCutoff,
                expiredBookingScanLimit);

        if (expiredBookingIds.isEmpty()) {
            return;
        }

        int expiredCount = 0;
        for (UUID bookingId : expiredBookingIds) {
            try {
                bookingService.expirePendingBooking(bookingId);
                expiredCount++;
            } catch (RuntimeException exception) {
                // A single damaged booking must not block cleanup for every other customer.
                log.error("Could not expire pending booking id={}", bookingId, exception);
            }
        }

        log.info("Expired {} of {} pending bookings after payment timeout", expiredCount, expiredBookingIds.size());
    }
}
