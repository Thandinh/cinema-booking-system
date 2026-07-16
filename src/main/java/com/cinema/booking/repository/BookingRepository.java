package com.cinema.booking.repository;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    // JOIN FETCH đầy đủ để tránh LazyInitializationException khi xử lý payment callback
    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.showtime s JOIN FETCH s.movie JOIN FETCH s.room r JOIN FETCH r.cinema " +
           "LEFT JOIN FETCH b.bookingDetails bd LEFT JOIN FETCH bd.seat " +
           "WHERE b.secureToken = :secureToken")
    Optional<Booking> findBySecureToken(@Param("secureToken") String secureToken);

    // Tách countQuery riêng để tránh Spring load toàn bộ data vào memory khi phân trang
    @Query(value = "SELECT b FROM Booking b JOIN FETCH b.user JOIN FETCH b.showtime s JOIN FETCH s.movie JOIN FETCH s.room r JOIN FETCH r.cinema WHERE b.user.id = :userId",
           countQuery = "SELECT COUNT(b) FROM Booking b WHERE b.user.id = :userId")
    Page<Booking> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query(value = "SELECT b FROM Booking b JOIN FETCH b.user JOIN FETCH b.showtime s JOIN FETCH s.movie JOIN FETCH s.room r JOIN FETCH r.cinema WHERE (:status IS NULL OR b.status = :status)",
           countQuery = "SELECT COUNT(b) FROM Booking b WHERE (:status IS NULL OR b.status = :status)")
    Page<Booking> findAllByStatus(@Param("status") BookingStatus status, Pageable pageable);

    @Query("""
            SELECT DISTINCT b FROM Booking b
            JOIN FETCH b.user
            JOIN FETCH b.showtime s
            JOIN FETCH s.movie
            JOIN FETCH s.room r
            JOIN FETCH r.cinema
            LEFT JOIN FETCH b.bookingDetails bd
            LEFT JOIN FETCH bd.seat
            LEFT JOIN FETCH bd.ticket
            WHERE b.id = :id
            """)
    Optional<Booking> findWithDetailsById(@Param("id") UUID id);

    @Query("""
            SELECT DISTINCT b FROM Booking b
            JOIN FETCH b.showtime st
            LEFT JOIN FETCH b.bookingDetails bd
            LEFT JOIN FETCH bd.seat
            WHERE b.status = :status
              AND (
                    (b.paymentExpiresAt IS NOT NULL AND b.paymentExpiresAt <= :now)
                    OR (b.paymentExpiresAt IS NULL AND b.createdAt < :legacyCutoff)
                  )
            """)
    List<Booking> findExpiredPendingBookings(@Param("status") BookingStatus status,
                                              @Param("now") LocalDateTime now,
                                              @Param("legacyCutoff") LocalDateTime legacyCutoff);

    /**
     * Query chuyên dùng cho tác vụ gửi Email.
     * JOIN FETCH tất cả các quan hệ cần thiết: user, showtime-movie-room-cinema,
     * bookingDetails-seat, ticket (có chứa QR code).
     * Được gọi trong luồng @Async nên phải load EAGER để tránh LazyInitializationException.
     */
    @Query("SELECT DISTINCT b FROM Booking b " +
           "JOIN FETCH b.user u " +
           "JOIN FETCH b.showtime s JOIN FETCH s.movie JOIN FETCH s.room r JOIN FETCH r.cinema " +
           "LEFT JOIN FETCH b.bookingDetails bd LEFT JOIN FETCH bd.seat LEFT JOIN FETCH bd.ticket " +
           "WHERE b.id = :id AND (s.isDeleted = false OR s.isDeleted IS NULL)")
    Optional<Booking> findByIdForEmail(@Param("id") UUID id);

    // ── Analytics ──────────────────────────────────────────────────────────────

    /** Số booking theo trạng thái */
    Long countByStatus(BookingStatus status);

    /** Số booking hôm nay */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.createdAt >= :startOfDay AND b.createdAt < :endOfDay")
    Long countBookingsToday(@Param("startOfDay") LocalDateTime startOfDay,
                            @Param("endOfDay")   LocalDateTime endOfDay);

    /**
     * Thống kê suất chiếu: số vé bán được, doanh thu.
     * Trả về: [showtimeId, movieTitle, roomName, cinemaName, startTime, totalSeats, bookedSeats, revenue]
     */
    @Query(value = """
            SELECT st.id                                                AS showtime_id,
                   m.title                                              AS movie_title,
                   ro.name                                              AS room_name,
                   ci.name                                              AS cinema_name,
                   TO_CHAR(st.start_time, 'YYYY-MM-DD HH24:MI')        AS start_time,
                   (SELECT COUNT(s.id) FROM seats s WHERE s.room_id = ro.id AND s.is_deleted = false) AS total_seats,
                   COUNT(t.id)                                          AS booked_seats,
                   COALESCE(SUM(p.amount / bd_count.cnt), 0)           AS revenue
            FROM showtimes st
            JOIN movies m         ON st.movie_id  = m.id
            JOIN rooms ro         ON st.room_id   = ro.id
            JOIN cinemas ci       ON ro.cinema_id = ci.id
            JOIN bookings b       ON b.showtime_id = st.id AND b.status = 'SUCCESS'
            JOIN booking_details bd ON bd.booking_id = b.id
            JOIN tickets t        ON t.booking_detail_id = bd.id
            JOIN payments p       ON p.booking_id = b.id AND p.status = 'SUCCESS'
            JOIN (SELECT booking_id, COUNT(*) AS cnt FROM booking_details GROUP BY booking_id) bd_count
                  ON bd_count.booking_id = b.id
            WHERE st.is_deleted = false
              AND (:cinemaId IS NULL OR ci.id = :cinemaId)
              AND (:from IS NULL OR st.start_time >= :from)
              AND (:to   IS NULL OR st.start_time <= :to)
            GROUP BY st.id, m.title, ro.name, ci.name, st.start_time, ro.id
            ORDER BY revenue DESC
            """, nativeQuery = true)
    Page<Object[]> findShowtimeStats(
            @Param("cinemaId") UUID cinemaId,
            @Param("from")     LocalDateTime from,
            @Param("to")       LocalDateTime to,
            Pageable pageable);
}
