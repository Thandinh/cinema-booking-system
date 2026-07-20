package com.cinema.booking.repository;

import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cinema.booking.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @Query(value = """
            SELECT p
            FROM Payment p
            JOIN FETCH p.booking b
            JOIN FETCH b.user u
            JOIN FETCH b.showtime st
            JOIN FETCH st.movie m
            JOIN FETCH st.room r
            JOIN FETCH r.cinema c
            WHERE b.user.id = :userId
            """,
           countQuery = "SELECT COUNT(p) FROM Payment p WHERE p.booking.user.id = :userId")
    Page<Payment> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query(value = """
            SELECT p
            FROM Payment p
            LEFT JOIN FETCH p.booking b
            LEFT JOIN FETCH b.user u
            LEFT JOIN FETCH b.showtime st
            LEFT JOIN FETCH st.movie m
            LEFT JOIN FETCH st.room r
            LEFT JOIN FETCH r.cinema c
            WHERE (:status IS NULL OR p.status = :status)
              AND (:method IS NULL OR p.method = :method)
              AND (
                    :keywordPattern IS NULL
                    OR LOWER(p.transactionNo) LIKE :keywordPattern
                    OR LOWER(CAST(b.id AS string)) LIKE :keywordPattern
                    OR LOWER(u.username) LIKE :keywordPattern
                    OR LOWER(u.email) LIKE :keywordPattern
                    OR LOWER(m.title) LIKE :keywordPattern
              )
            """,
           countQuery = """
            SELECT COUNT(p)
            FROM Payment p
            LEFT JOIN p.booking b
            LEFT JOIN b.user u
            LEFT JOIN b.showtime st
            LEFT JOIN st.movie m
            WHERE (:status IS NULL OR p.status = :status)
              AND (:method IS NULL OR p.method = :method)
              AND (
                    :keywordPattern IS NULL
                    OR LOWER(p.transactionNo) LIKE :keywordPattern
                    OR LOWER(CAST(b.id AS string)) LIKE :keywordPattern
                    OR LOWER(u.username) LIKE :keywordPattern
                    OR LOWER(u.email) LIKE :keywordPattern
                    OR LOWER(m.title) LIKE :keywordPattern
              )
            """)
    Page<Payment> findAllWithDetails(
            @Param("status") PaymentStatus status,
            @Param("method") com.cinema.booking.enums.PaymentMethod method,
            @Param("keywordPattern") String keywordPattern,
            Pageable pageable);

    Optional<Payment> findByTransactionNo(String transactionNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p JOIN FETCH p.booking b WHERE p.transactionNo = :transactionNo")
    Optional<Payment> findLockedByTransactionNo(@Param("transactionNo") String transactionNo);

    List<Payment> findByBookingIdInAndStatus(List<UUID> bookingIds, PaymentStatus status);

    // ── Analytics Queries ────────────────────────────────────────────────────

    /**
     * Tổng doanh thu theo trạng thái SUCCESS trong khoảng thời gian.
     * Dùng NULL-safe: nếu chưa có data thì trả về 0 thay vì NULL.
     */
    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.status = :status
              AND p.paymentTime BETWEEN :from AND :to
            """)
    BigDecimal sumRevenueBetween(
            @Param("status") PaymentStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * Doanh thu theo ngày trong khoảng thời gian — dùng cho Line Chart.
     * Trả về List<Object[]> gồm: [0]=dateLabel(String), [1]=revenue(BigDecimal), [2]=bookingCount(Long), [3]=ticketCount(Long)
     */
    @Query(value = """
            SELECT TO_CHAR(p.payment_time, 'YYYY-MM-DD') AS period,
                   COALESCE(SUM(p.amount), 0)             AS revenue,
                   COUNT(DISTINCT p.booking_id)            AS total_bookings,
                   COALESCE(SUM((
                       SELECT COUNT(t.id)
                       FROM booking_details bd
                       JOIN tickets t ON t.booking_detail_id = bd.id
                       WHERE bd.booking_id = p.booking_id
                   )), 0)                                  AS total_tickets
            FROM payments p
            WHERE p.status = 'SUCCESS'
              AND p.payment_time BETWEEN :from AND :to
            GROUP BY TO_CHAR(p.payment_time, 'YYYY-MM-DD')
            ORDER BY period ASC
            """, nativeQuery = true)
    List<Object[]> findDailyRevenueBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * Doanh thu theo tháng trong khoảng thời gian — dùng cho Line Chart.
     */
    @Query(value = """
            SELECT TO_CHAR(p.payment_time, 'YYYY-MM') AS period,
                   COALESCE(SUM(p.amount), 0)          AS revenue,
                   COUNT(DISTINCT p.booking_id)         AS total_bookings,
                   COALESCE(SUM((
                       SELECT COUNT(t.id)
                       FROM booking_details bd
                       JOIN tickets t ON t.booking_detail_id = bd.id
                       WHERE bd.booking_id = p.booking_id
                   )), 0)                               AS total_tickets
            FROM payments p
            WHERE p.status = 'SUCCESS'
              AND p.payment_time BETWEEN :from AND :to
            GROUP BY TO_CHAR(p.payment_time, 'YYYY-MM')
            ORDER BY period ASC
            """, nativeQuery = true)
    List<Object[]> findMonthlyRevenueBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * Top phim doanh thu cao nhất trong khoảng thời gian.
     * Trả về: [movieId, title, posterUrl, revenue, bookingCount, ticketCount]
     */
    @Query(value = """
            SELECT m.id                         AS movie_id,
                   m.title                      AS title,
                   m.poster_url                 AS poster_url,
                   COALESCE(SUM(p.amount), 0)   AS revenue,
                   COUNT(DISTINCT b.id)          AS total_bookings,
                   COALESCE(SUM((
                       SELECT COUNT(t.id)
                       FROM booking_details bd
                       JOIN tickets t ON t.booking_detail_id = bd.id
                       WHERE bd.booking_id = b.id
                   )), 0)                        AS total_tickets
            FROM payments p
            JOIN bookings b    ON p.booking_id  = b.id
            JOIN showtimes st  ON b.showtime_id = st.id
            JOIN movies m      ON st.movie_id   = m.id
            WHERE p.status = 'SUCCESS'
              AND p.payment_time BETWEEN :from AND :to
            GROUP BY m.id, m.title, m.poster_url
            ORDER BY revenue DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopMoviesByRevenue(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("limit") int limit);

    /**
     * Tổng doanh thu tất cả thời gian.
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = com.cinema.booking.enums.PaymentStatus.SUCCESS")
    BigDecimal sumTotalRevenue();
}
