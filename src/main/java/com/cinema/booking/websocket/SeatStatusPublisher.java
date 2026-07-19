package com.cinema.booking.websocket;

import com.cinema.booking.enums.SeatStatusType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service chuyên trách push SeatStatusEvent xuống tất cả client đang subscribe
 * topic "/topic/seatmap/{showtimeId}" qua WebSocket.
 *
 * Được inject vào BookingServiceImpl và HoldExpireScheduler để publish event
 * ngay sau mỗi thao tác đổi trạng thái ghế.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeatStatusPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    // =====================================================================
    // Publish cho 1 ghế (dùng khi HOLD: cần gửi thêm holdUntil + userId)
    // =====================================================================
    public void publishHold(UUID showtimeId, UUID seatId, UUID heldByUserId, LocalDateTime holdUntil) {
        SeatStatusEvent event = SeatStatusEvent.builder()
                .showtimeId(showtimeId)
                .seatId(seatId)
                .status(SeatStatusType.HOLD)
                .heldByUserId(heldByUserId)
                .holdUntil(holdUntil)
                .build();
        send(showtimeId, event);
    }

    // =====================================================================
    // Publish cho nhiều ghế cùng lúc (dùng khi BOOKED / AVAILABLE)
    // =====================================================================
    public void publishBulk(UUID showtimeId, List<UUID> seatIds, SeatStatusType status) {
        seatIds.forEach(seatId -> {
            SeatStatusEvent event = SeatStatusEvent.builder()
                    .showtimeId(showtimeId)
                    .seatId(seatId)
                    .status(status)
                    .build();
            send(showtimeId, event);
        });
    }

    public void publishAvailable(UUID showtimeId, List<UUID> seatIds) {
        publishBulk(showtimeId, seatIds, SeatStatusType.AVAILABLE);
    }

    // =====================================================================
    // Private helper: gửi đến đúng topic theo showtimeId
    // =====================================================================
    private void send(UUID showtimeId, SeatStatusEvent event) {
        String destination = "/topic/seatmap/" + showtimeId;
        messagingTemplate.convertAndSend(destination, event);
        log.debug("WS pushed {} → seat {} on showtime {}", event.getStatus(), event.getSeatId(), showtimeId);
    }
}
