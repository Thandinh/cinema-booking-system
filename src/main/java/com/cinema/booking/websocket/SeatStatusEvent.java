package com.cinema.booking.websocket;

import com.cinema.booking.enums.SeatStatusType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payload được push xuống client qua WebSocket mỗi khi trạng thái ghế thay đổi.
 *
 * Frontend nhận được object này và tự đổi màu ghế trên sơ đồ mà không cần reload trang.
 */
@Getter
@Builder
public class SeatStatusEvent {

    /** ID của suất chiếu — dùng để route event đến đúng topic */
    UUID showtimeId;

    /** ID của ghế vừa đổi trạng thái */
    UUID seatId;

    /** Trạng thái mới: AVAILABLE | HOLD | BOOKED */
    SeatStatusType status;

    /**
     * ID của người đang giữ ghế (chỉ có khi status = HOLD).
     * Frontend dùng để tô màu khác biệt: ghế của mình (vàng) vs ghế người khác giữ (xám).
     */
    UUID heldByUserId;

    /** Ghế hết hạn giữ lúc nào (chỉ có khi status = HOLD) */
    LocalDateTime holdUntil;

    /** Thời điểm server phát ra event — dùng để frontend debug hoặc sync clock */
    @Builder.Default
    LocalDateTime eventTime = LocalDateTime.now();
}
