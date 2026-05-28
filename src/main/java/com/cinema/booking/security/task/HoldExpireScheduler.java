package com.cinema.booking.security.task;

import com.cinema.booking.entity.SeatStatus;
import com.cinema.booking.enums.SeatStatusType;
import com.cinema.booking.repository.SeatStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler tự động nhả ghế về AVAILABLE khi hết thời gian giữ (10 phút).
 * Chạy mỗi 60 giây để đảm bảo không ghế nào bị kẹt quá lâu.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HoldExpireScheduler {

    private final SeatStatusRepository seatStatusRepository;

    @Scheduled(fixedDelay = 60_000) // Chạy mỗi 60 giây
    @Transactional
    public void releaseExpiredHolds() {
        List<SeatStatus> expired = seatStatusRepository.findExpiredHolds(
                SeatStatusType.HOLD, LocalDateTime.now());

        if (expired.isEmpty()) return;

        for (SeatStatus ss : expired) {
            ss.setStatus(SeatStatusType.AVAILABLE);
            ss.setHoldBy(null);
            ss.setHoldUntil(null);
        }

        seatStatusRepository.saveAll(expired);
        log.info("Released {} expired seat holds", expired.size());
    }
}
