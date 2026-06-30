package com.cinema.booking.service;

import com.cinema.booking.dto.response.DashboardSummaryResponse;
import com.cinema.booking.dto.response.RevenueByPeriodResponse;
import com.cinema.booking.dto.response.ShowtimeStatsResponse;
import com.cinema.booking.dto.response.TopMovieRevenueResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AnalyticsService {

    /**
     * Thẻ KPI tổng quan — gọi một lần để render toàn bộ Dashboard header.
     */
    DashboardSummaryResponse getDashboardSummary();

    /**
     * Doanh thu theo ngày trong khoảng thời gian.
     * @param from  ngày bắt đầu (inclusive)
     * @param to    ngày kết thúc (inclusive)
     */
    List<RevenueByPeriodResponse> getDailyRevenue(LocalDate from, LocalDate to);

    /**
     * Doanh thu theo tháng trong khoảng thời gian.
     */
    List<RevenueByPeriodResponse> getMonthlyRevenue(LocalDate from, LocalDate to);

    /**
     * Top N phim có doanh thu cao nhất.
     * @param from  từ ngày
     * @param to    đến ngày
     * @param limit số phim muốn lấy (mặc định 10)
     */
    List<TopMovieRevenueResponse> getTopMoviesByRevenue(LocalDate from, LocalDate to, int limit);

    /**
     * Thống kê từng suất chiếu: tỷ lệ lấp đầy, doanh thu, số vé.
     * @param cinemaId lọc theo rạp (null = tất cả rạp)
     * @param from     từ ngày
     * @param to       đến ngày
     */
    Page<ShowtimeStatsResponse> getShowtimeStats(UUID cinemaId, LocalDate from, LocalDate to, Pageable pageable);
}
