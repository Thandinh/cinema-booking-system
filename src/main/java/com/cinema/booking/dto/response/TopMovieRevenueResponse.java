package com.cinema.booking.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Top phim có doanh thu cao nhất trong khoảng thời gian.
 * Dùng cho biểu đồ Bar Chart / Top-N list.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopMovieRevenueResponse {
    UUID       movieId;
    String     title;
    String     posterUrl;
    BigDecimal revenue;
    Long       totalBookings;
    Long       totalTicketsSold;
}
