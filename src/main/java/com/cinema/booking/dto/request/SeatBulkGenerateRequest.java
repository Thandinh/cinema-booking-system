package com.cinema.booking.dto.request;

import com.cinema.booking.enums.SeatType;
import com.cinema.booking.enums.SeatLayoutTemplate;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Request tự động sinh sơ đồ ghế cho một phòng chiếu.
 *
 * Ví dụ:
 * {
 *   "roomId": "...",
 *   "rowLabels": ["A","B","C","D","E"],
 *   "seatsPerRow": 10,
 *   "seatType": "NORMAL",
 *   "priceMultiplier": 1.0
 * }
 * → Sinh ra 50 ghế: A1..A10, B1..B10, ..., E1..E10
 * → rowIndex và colIndex được tính tự động (0-based) cho frontend render grid
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeatBulkGenerateRequest {

    @NotNull(message = "ROOM_ID_REQUIRED")
    UUID roomId;

    /** Danh sách nhãn hàng theo thứ tự, ví dụ: ["A","B","C"] */
    @NotNull(message = "SEAT_ROW_REQUIRED")
    @Size(min = 1, max = 26, message = "SEAT_ROW_SIZE_INVALID")
    List<@NotBlank(message = "SEAT_ROW_REQUIRED") String> rowLabels;

    /** Số ghế mỗi hàng */
    @NotNull(message = "SEAT_NUMBER_REQUIRED")
    @Min(value = 1, message = "SEAT_NUMBER_INVALID")
    @Max(value = 50, message = "SEAT_NUMBER_INVALID")
    Integer seatsPerRow;

    @Builder.Default
    SeatLayoutTemplate layoutTemplate = SeatLayoutTemplate.CUSTOM;

    @Builder.Default
    SeatType seatType = SeatType.NORMAL;

    @DecimalMin(value = "0.0", message = "SEAT_MULTIPLIER_INVALID")
    @Builder.Default
    BigDecimal priceMultiplier = BigDecimal.ONE;

    /**
     * Nếu true → bỏ qua ghế đã tồn tại (idempotent).
     * Nếu false → ném lỗi nếu có ghế trùng.
     */
    @Builder.Default
    boolean skipExisting = true;
}
