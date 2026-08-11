package com.cinema.booking.repository;

import com.cinema.booking.enums.PaymentMethod;
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
              AND (:cinemaId IS NULL OR c.id = :cinemaId)
              AND (:city IS NULL OR c.city = :city)
              AND p.createdAt >= :fromTime
              AND p.createdAt < :toTime
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
            LEFT JOIN st.room r
            LEFT JOIN r.cinema c
            WHERE (:status IS NULL OR p.status = :status)
              AND (:method IS NULL OR p.method = :method)
              AND (:cinemaId IS NULL OR c.id = :cinemaId)
              AND (:city IS NULL OR c.city = :city)
              AND p.createdAt >= :fromTime
              AND p.createdAt < :toTime
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
            @Param("cinemaId") UUID cinemaId,
            @Param("city") String city,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            Pageable pageable);

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
              AND c.id IN :cinemaIds
              AND (:cinemaId IS NULL OR c.id = :cinemaId)
              AND (:city IS NULL OR c.city = :city)
              AND p.createdAt >= :fromTime
              AND p.createdAt < :toTime
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
            LEFT JOIN st.room r
            LEFT JOIN r.cinema c
            WHERE (:status IS NULL OR p.status = :status)
              AND (:method IS NULL OR p.method = :method)
              AND c.id IN :cinemaIds
              AND (:cinemaId IS NULL OR c.id = :cinemaId)
              AND (:city IS NULL OR c.city = :city)
              AND p.createdAt >= :fromTime
              AND p.createdAt < :toTime
              AND (
                    :keywordPattern IS NULL
                    OR LOWER(p.transactionNo) LIKE :keywordPattern
                    OR LOWER(CAST(b.id AS string)) LIKE :keywordPattern
                    OR LOWER(u.username) LIKE :keywordPattern
                    OR LOWER(u.email) LIKE :keywordPattern
                    OR LOWER(m.title) LIKE :keywordPattern
              )
            """)
    Page<Payment> findAllWithDetailsByCinemaIds(
            @Param("status") PaymentStatus status,
            @Param("method") com.cinema.booking.enums.PaymentMethod method,
            @Param("keywordPattern") String keywordPattern,
            @Param("cinemaId") UUID cinemaId,
            @Param("city") String city,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            @Param("cinemaIds") List<UUID> cinemaIds,
            Pageable pageable);

    Optional<Payment> findByTransactionNo(String transactionNo);

    Optional<Payment> findFirstByBookingIdAndMethodAndStatusOrderByCreatedAtDesc(
            UUID bookingId,
            PaymentMethod method,
            PaymentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Payment p
            WHERE p.booking.id = :bookingId
              AND p.status = com.cinema.booking.enums.PaymentStatus.PENDING
            ORDER BY p.createdAt DESC
            """)
    Optional<Payment> findLockedPendingByBookingId(@Param("bookingId") UUID bookingId);

    List<Payment> findByBookingIdAndStatus(UUID bookingId, PaymentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p JOIN FETCH p.booking b WHERE p.transactionNo = :transactionNo")
    Optional<Payment> findLockedByTransactionNo(@Param("transactionNo") String transactionNo);

    List<Payment> findByBookingIdInAndStatus(List<UUID> bookingIds, PaymentStatus status);

    @Query("""
            SELECT p
            FROM Payment p
            JOIN FETCH p.booking b
            WHERE b.id IN :bookingIds
              AND p.status = :status
            """)
    List<Payment> findWithBookingByBookingIdInAndStatus(@Param("bookingIds") List<UUID> bookingIds,
                                                        @Param("status") PaymentStatus status);

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
                   COALESCE(SUM(ticket_counts.ticket_count), 0) AS total_tickets
            FROM payments p
            LEFT JOIN (
                SELECT bd.booking_id, COUNT(t.id) AS ticket_count
                FROM booking_details bd
                JOIN tickets t ON t.booking_detail_id = bd.id
                GROUP BY bd.booking_id
            ) ticket_counts ON ticket_counts.booking_id = p.booking_id
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
                   COALESCE(SUM(ticket_counts.ticket_count), 0) AS total_tickets
            FROM payments p
            LEFT JOIN (
                SELECT bd.booking_id, COUNT(t.id) AS ticket_count
                FROM booking_details bd
                JOIN tickets t ON t.booking_detail_id = bd.id
                GROUP BY bd.booking_id
            ) ticket_counts ON ticket_counts.booking_id = p.booking_id
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
                   COALESCE(SUM(ticket_counts.ticket_count), 0) AS total_tickets
            FROM payments p
            JOIN bookings b    ON p.booking_id  = b.id
            JOIN showtimes st  ON b.showtime_id = st.id
            JOIN movies m      ON st.movie_id   = m.id
            LEFT JOIN (
                SELECT bd.booking_id, COUNT(t.id) AS ticket_count
                FROM booking_details bd
                JOIN tickets t ON t.booking_detail_id = bd.id
                GROUP BY bd.booking_id
            ) ticket_counts ON ticket_counts.booking_id = b.id
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
    @Query(value = """
            SELECT p.payment_time                                         AS paymentTime,
                   p.transaction_no                                      AS "transactionNo",
                   p.method                                              AS paymentMethod,
                   p.amount                                              AS amount,
                   b.id                                                  AS "bookingId",
                   b.status                                              AS "bookingStatus",
                   u.username                                            AS username,
                   u.email                                               AS email,
                   m.title                                               AS movieTitle,
                   c.name                                                AS cinemaName,
                   c.city                                                AS cinemaCity,
                   r.name                                                AS roomName,
                   st.start_time                                         AS showtimeStartTime,
                   COUNT(t.id)                                           AS ticketCount,
                   COALESCE(
                       STRING_AGG(
                           CONCAT(s.row_label, s.seat_number),
                           ', ' ORDER BY s.row_index, s.col_index
                       ),
                       ''
                   )                                                     AS seats
            FROM payments p
            JOIN bookings b          ON b.id = p.booking_id
            JOIN users u             ON u.id = b.user_id
            JOIN showtimes st        ON st.id = b.showtime_id
            JOIN movies m            ON m.id = st.movie_id
            JOIN rooms r             ON r.id = st.room_id
            JOIN cinemas c           ON c.id = r.cinema_id
            LEFT JOIN booking_details bd ON bd.booking_id = b.id
            LEFT JOIN seats s        ON s.id = bd.seat_id
            LEFT JOIN tickets t      ON t.booking_detail_id = bd.id
            WHERE p.status = 'SUCCESS'
              AND p.payment_time BETWEEN :from AND :to
              AND c.id = COALESCE(CAST(:cinemaId AS uuid), c.id)
              AND m.id = COALESCE(CAST(:movieId AS uuid), m.id)
            GROUP BY p.payment_time, p.transaction_no, p.method, p.amount,
                     b.id, b.status, u.username, u.email, m.title,
                     c.name, c.city, r.name, st.start_time
            ORDER BY p.payment_time DESC, b.id DESC
            """, nativeQuery = true)
    List<RevenueExportRow> findRevenueExportRows(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("cinemaId") String cinemaId,
            @Param("movieId") String movieId);

    @Query(value = """
            SELECT *
            FROM (
                SELECT 'PENDING_PAYMENT_EXPIRED' AS "issueType",
                       'HIGH'                    AS "severity",
                       b.id                      AS "bookingId",
                       p.id                      AS "paymentId",
                       p.transaction_no          AS "transactionNo",
                       b.status                  AS "bookingStatus",
                       p.status                  AS "paymentStatus",
                       'Payment is still pending after booking payment window expired' AS "message",
                       COALESCE(b.payment_expires_at, p.created_at) AS "createdAt"
                FROM payments p
                JOIN bookings b ON b.id = p.booking_id
                WHERE p.status = 'PENDING'
                  AND b.status = 'PENDING'
                  AND b.payment_expires_at IS NOT NULL
                  AND b.payment_expires_at <= :now

                UNION ALL

                SELECT 'PAYMENT_SUCCESS_BOOKING_NOT_SUCCESS' AS "issueType",
                       'CRITICAL'                            AS "severity",
                       b.id                                  AS "bookingId",
                       p.id                                  AS "paymentId",
                       p.transaction_no                      AS "transactionNo",
                       b.status                              AS "bookingStatus",
                       p.status                              AS "paymentStatus",
                       'Payment succeeded but booking is not marked SUCCESS' AS "message",
                       p.payment_time                        AS "createdAt"
                FROM payments p
                JOIN bookings b ON b.id = p.booking_id
                WHERE p.status = 'SUCCESS'
                  AND b.status <> 'SUCCESS'

                UNION ALL

                SELECT 'BOOKING_SUCCESS_WITHOUT_SUCCESS_PAYMENT' AS "issueType",
                       'CRITICAL'                                AS "severity",
                       b.id                                      AS "bookingId",
                       NULL::uuid                                AS "paymentId",
                       NULL::varchar                             AS "transactionNo",
                       b.status                                  AS "bookingStatus",
                       NULL::varchar                             AS "paymentStatus",
                       'Booking is SUCCESS but there is no successful payment' AS "message",
                       b.updated_at                              AS "createdAt"
                FROM bookings b
                WHERE b.status = 'SUCCESS'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM payments p
                      WHERE p.booking_id = b.id
                        AND p.status = 'SUCCESS'
                  )

                UNION ALL

                SELECT 'BOOKING_SUCCESS_MISSING_TICKETS' AS "issueType",
                       'HIGH'                            AS "severity",
                       b.id                              AS "bookingId",
                       NULL::uuid                        AS "paymentId",
                       NULL::varchar                     AS "transactionNo",
                       b.status                          AS "bookingStatus",
                       NULL::varchar                     AS "paymentStatus",
                       'Booking is SUCCESS but ticket count does not match booked seats' AS "message",
                       b.updated_at                      AS "createdAt"
                FROM bookings b
                WHERE b.status = 'SUCCESS'
                  AND (
                      SELECT COUNT(*)
                      FROM booking_details bd
                      WHERE bd.booking_id = b.id
                  ) <> (
                      SELECT COUNT(t.id)
                      FROM booking_details bd
                      JOIN tickets t ON t.booking_detail_id = bd.id
                      WHERE bd.booking_id = b.id
                  )

                UNION ALL

                SELECT 'PENDING_PAYMENT_BOOKING_FINALIZED' AS "issueType",
                       'MEDIUM'                            AS "severity",
                       b.id                                AS "bookingId",
                       p.id                                AS "paymentId",
                       p.transaction_no                    AS "transactionNo",
                       b.status                            AS "bookingStatus",
                       p.status                            AS "paymentStatus",
                       'Payment remains PENDING while booking is already finalized' AS "message",
                       p.created_at                        AS "createdAt"
                FROM payments p
                JOIN bookings b ON b.id = p.booking_id
                WHERE p.status = 'PENDING'
                  AND b.status <> 'PENDING'
            ) issues
            ORDER BY
                CASE severity
                    WHEN 'CRITICAL' THEN 1
                    WHEN 'HIGH' THEN 2
                    WHEN 'MEDIUM' THEN 3
                    ELSE 4
                END,
                "createdAt" DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<PaymentReconciliationIssueRow> findReconciliationIssues(
            @Param("now") LocalDateTime now,
            @Param("limit") int limit);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = com.cinema.booking.enums.PaymentStatus.SUCCESS")
    BigDecimal sumTotalRevenue();

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.status = com.cinema.booking.enums.PaymentStatus.SUCCESS
              AND p.booking.showtime.room.cinema.id IN :cinemaIds
            """)
    BigDecimal sumTotalRevenueByCinemaIds(@Param("cinemaIds") List<UUID> cinemaIds);

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.status = :status
              AND p.paymentTime BETWEEN :from AND :to
              AND p.booking.showtime.room.cinema.id IN :cinemaIds
            """)
    BigDecimal sumRevenueBetweenByCinemaIds(
            @Param("status") PaymentStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("cinemaIds") List<UUID> cinemaIds);

    @Query(value = """
            SELECT TO_CHAR(p.payment_time, 'YYYY-MM-DD') AS period,
                   COALESCE(SUM(p.amount), 0)             AS revenue,
                   COUNT(DISTINCT p.booking_id)            AS total_bookings,
                   COALESCE(SUM(ticket_counts.ticket_count), 0) AS total_tickets
            FROM payments p
            JOIN bookings b ON b.id = p.booking_id
            JOIN showtimes st ON st.id = b.showtime_id
            JOIN rooms r ON r.id = st.room_id
            LEFT JOIN (
                SELECT bd.booking_id, COUNT(t.id) AS ticket_count
                FROM booking_details bd
                JOIN tickets t ON t.booking_detail_id = bd.id
                GROUP BY bd.booking_id
            ) ticket_counts ON ticket_counts.booking_id = p.booking_id
            WHERE p.status = 'SUCCESS'
              AND p.payment_time BETWEEN :from AND :to
              AND r.cinema_id IN (:cinemaIds)
            GROUP BY TO_CHAR(p.payment_time, 'YYYY-MM-DD')
            ORDER BY period ASC
            """, nativeQuery = true)
    List<Object[]> findDailyRevenueBetweenByCinemaIds(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("cinemaIds") List<UUID> cinemaIds);

    @Query(value = """
            SELECT TO_CHAR(p.payment_time, 'YYYY-MM') AS period,
                   COALESCE(SUM(p.amount), 0)          AS revenue,
                   COUNT(DISTINCT p.booking_id)         AS total_bookings,
                   COALESCE(SUM(ticket_counts.ticket_count), 0) AS total_tickets
            FROM payments p
            JOIN bookings b ON b.id = p.booking_id
            JOIN showtimes st ON st.id = b.showtime_id
            JOIN rooms r ON r.id = st.room_id
            LEFT JOIN (
                SELECT bd.booking_id, COUNT(t.id) AS ticket_count
                FROM booking_details bd
                JOIN tickets t ON t.booking_detail_id = bd.id
                GROUP BY bd.booking_id
            ) ticket_counts ON ticket_counts.booking_id = p.booking_id
            WHERE p.status = 'SUCCESS'
              AND p.payment_time BETWEEN :from AND :to
              AND r.cinema_id IN (:cinemaIds)
            GROUP BY TO_CHAR(p.payment_time, 'YYYY-MM')
            ORDER BY period ASC
            """, nativeQuery = true)
    List<Object[]> findMonthlyRevenueBetweenByCinemaIds(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("cinemaIds") List<UUID> cinemaIds);

    @Query(value = """
            SELECT m.id                         AS movie_id,
                   m.title                      AS title,
                   m.poster_url                 AS poster_url,
                   COALESCE(SUM(p.amount), 0)   AS revenue,
                   COUNT(DISTINCT b.id)          AS total_bookings,
                   COALESCE(SUM(ticket_counts.ticket_count), 0) AS total_tickets
            FROM payments p
            JOIN bookings b    ON p.booking_id  = b.id
            JOIN showtimes st  ON b.showtime_id = st.id
            JOIN rooms r       ON st.room_id    = r.id
            JOIN movies m      ON st.movie_id   = m.id
            LEFT JOIN (
                SELECT bd.booking_id, COUNT(t.id) AS ticket_count
                FROM booking_details bd
                JOIN tickets t ON t.booking_detail_id = bd.id
                GROUP BY bd.booking_id
            ) ticket_counts ON ticket_counts.booking_id = b.id
            WHERE p.status = 'SUCCESS'
              AND p.payment_time BETWEEN :from AND :to
              AND r.cinema_id IN (:cinemaIds)
            GROUP BY m.id, m.title, m.poster_url
            ORDER BY revenue DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopMoviesByRevenueByCinemaIds(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("cinemaIds") List<UUID> cinemaIds,
            @Param("limit") int limit);
}


