package com.cinema.booking.security.task;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PendingBookingExpireScheduler {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    @Value("${booking.pending-timeout-minutes:15}")
    int pendingTimeoutMinutes;

    @Scheduled(fixedDelay = 60_000)
    public void expirePendingBookings() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime legacyCutoff = now.minusMinutes(pendingTimeoutMinutes);
        List<Booking> expiredBookings = bookingRepository.findExpiredPendingBookings(
                BookingStatus.PENDING, now, legacyCutoff);

        if (expiredBookings.isEmpty()) {
            return;
        }

        for (Booking booking : expiredBookings) {
            bookingService.expirePendingBooking(booking.getId());
        }

        log.info("Expired {} pending bookings after {} minutes",
                expiredBookings.size(), pendingTimeoutMinutes);
    }
}
