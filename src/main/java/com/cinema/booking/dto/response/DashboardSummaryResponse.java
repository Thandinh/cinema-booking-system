package com.cinema.booking.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/**
 * Thẻ KPI tổng quan cho Admin Dashboard.
 * Trả về một đối tượng duy nhất chứa toàn bộ số liệu tóm tắt
 * giúp Frontend render được trang Dashboard với 1 request duy nhất.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardSummaryResponse {

    // ── Doanh thu ────────────────────────────────
    BigDecimal totalRevenue;
    BigDecimal revenueToday;
    BigDecimal revenueThisMonth;
    /** % tăng/giảm so với tháng trước (null nếu chưa có dữ liệu tháng trước) */
    BigDecimal revenueGrowthPercent;

    // ── Đặt vé ──────────────────────────────────
    Long totalBookings;
    Long bookingsToday;
    Long pendingBookings;
    Long successBookings;
    Long failedBookings;
    Long cancelledBookings;
    Long expiredBookings;

    // ── Người dùng ──────────────────────────────
    Long totalUsers;
    Long newUsersToday;
    Long newUsersThisMonth;

    // ── Nội dung ────────────────────────────────
    Long totalMovies;
    Long activeMovies;
    Long totalShowtimes;
    Long upcomingShowtimes;

    // ── Tickets ─────────────────────────────────
    Long totalTickets;
    Long ticketsCheckedIn;
}
