package com.cinema.booking.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Thống kê tổng quan theo từng Suất chiếu (Showtime).
 * Dùng để Admin xem hiệu quả từng suất: tỷ lệ lấp đầy ghế, doanh thu.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShowtimeStatsResponse {
    UUID       showtimeId;
    String     movieTitle;
    String     roomName;
    String     cinemaName;
    String     startTime;
    /** Tổng số ghế của phòng */
    Integer    totalSeats;
    /** Số ghế đã bán thành công */
    Long       bookedSeats;
    /** Tỷ lệ lấp đầy: bookedSeats / totalSeats * 100 */
    BigDecimal occupancyRate;
    BigDecimal revenue;
}
