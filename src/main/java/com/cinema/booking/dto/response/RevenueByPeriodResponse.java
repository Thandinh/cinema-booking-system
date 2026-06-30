package com.cinema.booking.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/**
 * Doanh thu theo từng ngày/tháng trong khoảng thời gian.
 * Dùng cho biểu đồ Line Chart ở Admin Dashboard.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RevenueByPeriodResponse {
    /** Nhãn thời gian — tuỳ theo granularity: "2026-06-18" / "2026-06" */
    String period;
    BigDecimal revenue;
    Long     totalBookings;
    Long     totalTickets;
}
