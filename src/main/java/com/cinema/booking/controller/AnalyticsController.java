package com.cinema.booking.controller;

import com.cinema.booking.dto.response.*;
import com.cinema.booking.service.AnalyticsService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Admin Dashboard Analytics API.
 * Tất cả endpoints đều yêu cầu quyền ANALYTICS_VIEW (chỉ ADMIN/MANAGER mới có).
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AnalyticsController {

    AnalyticsService analyticsService;

    /**
     * GET /api/v1/analytics/summary
     * Tổng hợp KPI: tổng doanh thu, số booking, số user, số phim, số vé...
     * Frontend gọi đúng 1 request để render toàn bộ thẻ thống kê.
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    public ApiResponse<DashboardSummaryResponse> getDashboardSummary() {
        return ApiResponse.<DashboardSummaryResponse>builder()
                .code(1000)
                .result(analyticsService.getDashboardSummary())
                .build();
    }

    /**
     * GET /api/v1/analytics/revenue/daily?from=2026-06-01&to=2026-06-18
     * Doanh thu theo từng ngày — dùng để vẽ Line Chart.
     * Mặc định: 30 ngày gần nhất nếu không truyền param.
     */
    @GetMapping("/revenue/daily")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    public ApiResponse<List<RevenueByPeriodResponse>> getDailyRevenue(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDate effectiveTo   = to   != null ? to   : LocalDate.now();
        LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(29);

        return ApiResponse.<List<RevenueByPeriodResponse>>builder()
                .code(1000)
                .result(analyticsService.getDailyRevenue(effectiveFrom, effectiveTo))
                .build();
    }

    /**
     * GET /api/v1/analytics/revenue/monthly?from=2026-01-01&to=2026-12-31
     * Doanh thu theo từng tháng — dùng để vẽ Line/Bar Chart.
     * Mặc định: 12 tháng gần nhất nếu không truyền param.
     */
    @GetMapping("/revenue/monthly")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    public ApiResponse<List<RevenueByPeriodResponse>> getMonthlyRevenue(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDate effectiveTo   = to   != null ? to   : LocalDate.now();
        LocalDate effectiveFrom = from != null ? from : effectiveTo.minusMonths(11).withDayOfMonth(1);

        return ApiResponse.<List<RevenueByPeriodResponse>>builder()
                .code(1000)
                .result(analyticsService.getMonthlyRevenue(effectiveFrom, effectiveTo))
                .build();
    }

    /**
     * GET /api/v1/analytics/movies/top-revenue?from=2026-01-01&to=2026-12-31&limit=10
     * Top phim doanh thu cao nhất — dùng để vẽ Horizontal Bar Chart hoặc Leaderboard.
     */
    @GetMapping("/movies/top-revenue")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    public ApiResponse<List<TopMovieRevenueResponse>> getTopMoviesByRevenue(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit) {

        LocalDate effectiveTo   = to   != null ? to   : LocalDate.now();
        LocalDate effectiveFrom = from != null ? from : effectiveTo.minusMonths(1);

        return ApiResponse.<List<TopMovieRevenueResponse>>builder()
                .code(1000)
                .result(analyticsService.getTopMoviesByRevenue(effectiveFrom, effectiveTo, limit))
                .build();
    }

    /**
     * GET /api/v1/analytics/showtimes?cinemaId=...&from=2026-06-01&to=2026-06-18&page=0&size=20
     * Thống kê chi tiết từng suất chiếu: tỷ lệ lấp đầy ghế, doanh thu.
     * Hỗ trợ lọc theo rạp và khoảng thời gian, phân trang.
     */
    @GetMapping("/showtimes")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    public ApiResponse<Page<ShowtimeStatsResponse>> getShowtimeStats(
            @RequestParam(required = false) UUID cinemaId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable) {

        return ApiResponse.<Page<ShowtimeStatsResponse>>builder()
                .code(1000)
                .result(analyticsService.getShowtimeStats(cinemaId, from, to, pageable))
                .build();
    }
}
