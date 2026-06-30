package com.cinema.booking.security.task;

import com.cinema.booking.entity.SeatStatus;
import com.cinema.booking.enums.SeatStatusType;
import com.cinema.booking.repository.SeatStatusRepository;
import com.cinema.booking.websocket.SeatStatusPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Scheduler tự động nhả ghế về AVAILABLE khi hết thời gian giữ (10 phút).
 * Chạy mỗi 60 giây để đảm bảo không ghế nào bị kẹt quá lâu.
 * Sau khi nhả ghế, push WebSocket event để frontend tự cập nhật màu ghế.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HoldExpireScheduler {

    private final SeatStatusRepository seatStatusRepository;
    private final SeatStatusPublisher  seatStatusPublisher;

    @Scheduled(fixedDelay = 60_000) // Chạy mỗi 60 giây
    @Transactional
    public void releaseExpiredHolds() {
        List<SeatStatus> expired = seatStatusRepository.findExpiredHolds(
                SeatStatusType.HOLD, LocalDateTime.now());

        if (expired.isEmpty()) return;

        // Nhóm theo showtimeId để gọi publishBulk hiệu quả hơn
        Map<UUID, List<UUID>> byShowtime = expired.stream()
                .collect(Collectors.groupingBy(
                        ss -> ss.getShowtime().getId(),
                        Collectors.mapping(ss -> ss.getSeat().getId(), Collectors.toList())
                ));

        for (SeatStatus ss : expired) {
            ss.setStatus(SeatStatusType.AVAILABLE);
            ss.setHoldBy(null);
            ss.setHoldUntil(null);
        }
        seatStatusRepository.saveAll(expired);
        log.info("Released {} expired seat holds", expired.size());

        // ── WS: Push AVAILABLE cho từng nhóm showtime ──
        byShowtime.forEach((showtimeId, seatIds) ->
                seatStatusPublisher.publishBulk(showtimeId, seatIds, SeatStatusType.AVAILABLE));
    }
}
