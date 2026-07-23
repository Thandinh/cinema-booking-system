package com.cinema.booking.service.impl;

import com.cinema.booking.dto.response.*;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.enums.MovieStatus;
import com.cinema.booking.enums.PaymentStatus;
import com.cinema.booking.enums.TicketStatus;
import com.cinema.booking.repository.*;
import com.cinema.booking.service.AnalyticsService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final DateTimeFormatter CSV_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    PaymentRepository  paymentRepository;
    BookingRepository  bookingRepository;
    UserRepository     userRepository;
    MovieRepository    movieRepository;
    ShowtimeRepository showtimeRepository;
    TicketRepository   ticketRepository;

    // =========================================================================
    // DASHBOARD SUMMARY
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(value = "dashboardSummary", unless = "#result == null")
    public DashboardSummaryResponse getDashboardSummary() {
        // Boundaries thời gian
        LocalDateTime startOfToday     = LocalDate.now().atStartOfDay();
        LocalDateTime endOfToday       = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime startOfMonth     = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime endOfMonth       = YearMonth.now().atEndOfMonth().atTime(LocalTime.MAX);
        LocalDateTime startOfLastMonth = YearMonth.now().minusMonths(1).atDay(1).atStartOfDay();
        LocalDateTime endOfLastMonth   = YearMonth.now().minusMonths(1).atEndOfMonth().atTime(LocalTime.MAX);

        // Doanh thu
        BigDecimal totalRevenue      = paymentRepository.sumTotalRevenue();
        BigDecimal revenueToday      = paymentRepository.sumRevenueBetween(PaymentStatus.SUCCESS, startOfToday, endOfToday);
        BigDecimal revenueThisMonth  = paymentRepository.sumRevenueBetween(PaymentStatus.SUCCESS, startOfMonth, endOfMonth);
        BigDecimal revenueLastMonth  = paymentRepository.sumRevenueBetween(PaymentStatus.SUCCESS, startOfLastMonth, endOfLastMonth);

        BigDecimal growthPercent = null;
        if (revenueLastMonth != null && revenueLastMonth.compareTo(BigDecimal.ZERO) > 0) {
            growthPercent = revenueThisMonth.subtract(revenueLastMonth)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(revenueLastMonth, 2, RoundingMode.HALF_UP);
        }

        // Bookings
        Long bookingsToday    = bookingRepository.countBookingsToday(startOfToday, endOfToday);
        Long pendingBookings  = bookingRepository.countByStatus(BookingStatus.PENDING);
        Long successBookings  = bookingRepository.countByStatus(BookingStatus.SUCCESS);
        Long failedBookings   = bookingRepository.countByStatus(BookingStatus.FAILED);
        Long cancelledBookings= bookingRepository.countByStatus(BookingStatus.CANCELLED);
        Long expiredBookings  = bookingRepository.countByStatus(BookingStatus.EXPIRED);
        Long totalBookings    = pendingBookings + successBookings + failedBookings + cancelledBookings + expiredBookings;

        // Users
        Long totalUsers       = userRepository.countActiveUsers();
        Long newUsersToday    = userRepository.countNewUsersToday(startOfToday, endOfToday);
        Long newUsersThisMonth= userRepository.countNewUsersThisMonth(startOfMonth, endOfMonth);

        // Movies & Showtimes
        Long totalMovies      = movieRepository.countByIsDeletedFalse();
        Long activeMovies     = movieRepository.countByStatusAndIsDeletedFalse(MovieStatus.NOW_SHOWING);
        Long totalShowtimes   = showtimeRepository.countByIsDeletedFalse();
        Long upcomingShowtimes= showtimeRepository.countUpcomingShowtimes();

        // Tickets
        Long totalTickets     = ticketRepository.count();
        Long checkedIn        = ticketRepository.countByStatus(TicketStatus.USED);

        return DashboardSummaryResponse.builder()
                .totalRevenue(totalRevenue)
                .revenueToday(revenueToday)
                .revenueThisMonth(revenueThisMonth)
                .revenueGrowthPercent(growthPercent)
                .totalBookings(totalBookings)
                .bookingsToday(bookingsToday)
                .pendingBookings(pendingBookings)
                .successBookings(successBookings)
                .failedBookings(failedBookings)
                .cancelledBookings(cancelledBookings)
                .expiredBookings(expiredBookings)
                .totalUsers(totalUsers)
                .newUsersToday(newUsersToday)
                .newUsersThisMonth(newUsersThisMonth)
                .totalMovies(totalMovies)
                .activeMovies(activeMovies)
                .totalShowtimes(totalShowtimes)
                .upcomingShowtimes(upcomingShowtimes)
                .totalTickets(totalTickets)
                .ticketsCheckedIn(checkedIn)
                .build();
    }

    // =========================================================================
    // REVENUE CHARTS
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public List<RevenueByPeriodResponse> getDailyRevenue(LocalDate from, LocalDate to) {
        List<Object[]> rows = paymentRepository.findDailyRevenueBetween(
                from.atStartOfDay(), to.atTime(LocalTime.MAX));
        return rows.stream().map(this::mapToRevenueByPeriod).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RevenueByPeriodResponse> getMonthlyRevenue(LocalDate from, LocalDate to) {
        List<Object[]> rows = paymentRepository.findMonthlyRevenueBetween(
                from.atStartOfDay(), to.atTime(LocalTime.MAX));
        return rows.stream().map(this::mapToRevenueByPeriod).toList();
    }

    // =========================================================================
    // TOP MOVIES
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public List<TopMovieRevenueResponse> getTopMoviesByRevenue(LocalDate from, LocalDate to, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50); // clamp 1–50
        List<Object[]> rows = paymentRepository.findTopMoviesByRevenue(
                from.atStartOfDay(), to.atTime(LocalTime.MAX), safeLimit);
        return rows.stream().map(this::mapToTopMovie).toList();
    }

    // =========================================================================
    // SHOWTIME STATS
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public Page<ShowtimeStatsResponse> getShowtimeStats(UUID cinemaId, LocalDate from, LocalDate to, Pageable pageable) {
        LocalDateTime dtFrom = from != null ? from.atStartOfDay()      : null;
        LocalDateTime dtTo   = to   != null ? to.atTime(LocalTime.MAX) : null;
        return bookingRepository.findShowtimeStats(cinemaId, dtFrom, dtTo, pageable)
                .map(this::mapToShowtimeStats);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportRevenueCsv(LocalDate from, LocalDate to, UUID cinemaId, UUID movieId) {
        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(29);
        if (effectiveFrom.isAfter(effectiveTo)) {
            LocalDate tmp = effectiveFrom;
            effectiveFrom = effectiveTo;
            effectiveTo = tmp;
        }

        List<RevenueExportRow> rows = paymentRepository.findRevenueExportRows(
                effectiveFrom.atStartOfDay(),
                effectiveTo.atTime(LocalTime.MAX),
                cinemaId != null ? cinemaId.toString() : null,
                movieId != null ? movieId.toString() : null);

        StringBuilder csv = new StringBuilder(1024 + rows.size() * 256);
        csv.append('\ufeff');
        appendCsvRow(csv, List.of(
                "Payment Time",
                "Transaction No",
                "Method",
                "Amount VND",
                "Booking ID",
                "Booking Status",
                "Username",
                "Email",
                "Movie",
                "Cinema",
                "City",
                "Room",
                "Showtime",
                "Ticket Count",
                "Seats"));

        for (RevenueExportRow row : rows) {
            appendCsvRow(csv, Arrays.asList(
                    formatCsvDateTime(row.getPaymentTime()),
                    row.getTransactionNo(),
                    row.getPaymentMethod(),
                    formatCsvMoney(row.getAmount()),
                    row.getBookingId() != null ? row.getBookingId().toString() : "",
                    row.getBookingStatus(),
                    row.getUsername(),
                    row.getEmail(),
                    row.getMovieTitle(),
                    row.getCinemaName(),
                    row.getCinemaCity(),
                    row.getRoomName(),
                    formatCsvDateTime(row.getShowtimeStartTime()),
                    String.valueOf(row.getTicketCount() != null ? row.getTicketCount() : 0),
                    row.getSeats()));
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    // =========================================================================
    // PRIVATE MAPPERS
    // =========================================================================

    private RevenueByPeriodResponse mapToRevenueByPeriod(Object[] row) {
        // row: [period(String), revenue(Number), totalBookings(Number), totalTickets(Number)]
        return RevenueByPeriodResponse.builder()
                .period(String.valueOf(row[0]))
                .revenue(toBigDecimal(row[1]))
                .totalBookings(toLong(row[2]))
                .totalTickets(toLong(row[3]))
                .build();
    }

    private TopMovieRevenueResponse mapToTopMovie(Object[] row) {
        // row: [movieId(UUID/String), title, posterUrl, revenue, bookingCount, ticketCount]
        return TopMovieRevenueResponse.builder()
                .movieId(toUUID(row[0]))
                .title(String.valueOf(row[1]))
                .posterUrl(row[2] != null ? String.valueOf(row[2]) : null)
                .revenue(toBigDecimal(row[3]))
                .totalBookings(toLong(row[4]))
                .totalTicketsSold(toLong(row[5]))
                .build();
    }

    private ShowtimeStatsResponse mapToShowtimeStats(Object[] row) {
        // row: [showtimeId, movieTitle, roomName, cinemaName, startTime, totalSeats, bookedSeats, revenue]
        int   totalSeats  = row[5] != null ? ((Number) row[5]).intValue() : 0;
        long  bookedSeats = toLong(row[6]);
        BigDecimal occupancy = totalSeats > 0
                ? BigDecimal.valueOf(bookedSeats)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalSeats), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return ShowtimeStatsResponse.builder()
                .showtimeId(toUUID(row[0]))
                .movieTitle(String.valueOf(row[1]))
                .roomName(String.valueOf(row[2]))
                .cinemaName(String.valueOf(row[3]))
                .startTime(String.valueOf(row[4]))
                .totalSeats(totalSeats)
                .bookedSeats(bookedSeats)
                .occupancyRate(occupancy)
                .revenue(toBigDecimal(row[7]))
                .build();
    }

    // ── Type conversion helpers ────────────────────────────────────────────────

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal bd) return bd;
        return new BigDecimal(val.toString());
    }

    private Long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Long l) return l;
        if (val instanceof Number n) return n.longValue();
        return Long.parseLong(val.toString());
    }

    private UUID toUUID(Object val) {
        if (val == null) return null;
        if (val instanceof UUID u) return u;
        return UUID.fromString(val.toString());
    }

    private void appendCsvRow(StringBuilder csv, List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                csv.append(',');
            }
            csv.append(escapeCsv(values.get(index)));
        }
        csv.append('\n');
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n');
        boolean mustQuote = normalized.contains(",") || normalized.contains("\"") || normalized.contains("\n");
        String escaped = normalized.replace("\"", "\"\"");
        return mustQuote ? "\"" + escaped + "\"" : escaped;
    }

    private String formatCsvDateTime(LocalDateTime value) {
        return value != null ? value.format(CSV_DATE_TIME_FORMATTER) : "";
    }

    private String formatCsvMoney(BigDecimal value) {
        return value != null ? value.stripTrailingZeros().toPlainString() : "0";
    }
}
