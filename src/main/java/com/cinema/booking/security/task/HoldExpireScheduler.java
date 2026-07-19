package com.cinema.booking.security.task;

import com.cinema.booking.repository.ExpiredSeatHoldProjection;
import com.cinema.booking.repository.SeatStatusRepository;
import com.cinema.booking.websocket.SeatStatusPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class HoldExpireScheduler {

    private final SeatStatusRepository seatStatusRepository;
    private final SeatStatusPublisher seatStatusPublisher;

    @Value("${booking.expired-hold-scan-limit:500}")
    int expiredHoldScanLimit;

    @Scheduled(fixedDelayString = "${booking.expired-hold-scan-delay-ms:30000}")
    @Transactional
    public void releaseExpiredHolds() {
        List<ExpiredSeatHoldProjection> expired = seatStatusRepository.findExpiredHoldRows(
                LocalDateTime.now(), expiredHoldScanLimit);

        if (expired.isEmpty()) {
            return;
        }

        Map<UUID, List<UUID>> seatIdsByShowtime = expired.stream()
                .collect(Collectors.groupingBy(
                        ExpiredSeatHoldProjection::getShowtimeId,
                        Collectors.mapping(ExpiredSeatHoldProjection::getSeatId, Collectors.toList())
                ));

        List<UUID> expiredIds = expired.stream()
                .map(ExpiredSeatHoldProjection::getId)
                .toList();

        int releasedCount = seatStatusRepository.releaseExpiredHoldsByIds(expiredIds);
        log.info("Released {} expired seat holds", releasedCount);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                seatIdsByShowtime.forEach(seatStatusPublisher::publishAvailable);
            }
        });
    }
}
