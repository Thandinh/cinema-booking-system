package com.cinema.booking.security.task;

import com.cinema.booking.service.ShowtimeStatusSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "booking.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ShowtimeStatusSyncScheduler {

    private final ShowtimeStatusSyncService showtimeStatusSyncService;

    @Scheduled(fixedDelayString = "${showtime.status-sync-delay-ms:60000}")
    public void synchronizeShowtimeStatuses() {
        showtimeStatusSyncService.synchronizeCurrentStatuses();
    }
}
