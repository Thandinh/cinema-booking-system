package com.cinema.booking.service.impl;

import com.cinema.booking.repository.ShowtimeRepository;
import com.cinema.booking.service.ShowtimeStatusSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShowtimeStatusSyncServiceImpl implements ShowtimeStatusSyncService {

    private final ShowtimeRepository showtimeRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int synchronizeCurrentStatuses() {
        LocalDateTime now = LocalDateTime.now();
        int endedCount = showtimeRepository.markFinishedShowtimesAsEnded(now);
        int ongoingCount = showtimeRepository.markStartedShowtimesAsOngoing(now);
        int updatedCount = endedCount + ongoingCount;

        if (updatedCount > 0) {
            log.info("Synchronized showtime statuses: ongoing={}, ended={}", ongoingCount, endedCount);
        }
        return updatedCount;
    }
}
