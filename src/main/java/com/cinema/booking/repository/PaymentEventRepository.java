package com.cinema.booking.repository;

import com.cinema.booking.entity.PaymentEvent;
import com.cinema.booking.enums.PaymentEventType;
import com.cinema.booking.repository.projection.PaymentEventSummaryRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, UUID> {

    @Query("""
            SELECT e
            FROM PaymentEvent e
            WHERE (:bookingId IS NULL OR e.bookingId = :bookingId)
              AND (:paymentId IS NULL OR e.paymentId = :paymentId)
              AND (:eventType IS NULL OR e.eventType = :eventType)
              AND (:success IS NULL OR e.success = :success)
              AND e.createdAt >= :fromTime
              AND e.createdAt < :toTime
              AND (
                    :keyword IS NULL
                    OR LOWER(e.transactionNo) LIKE :keyword
                    OR LOWER(e.message) LIKE :keyword
                    OR LOWER(CAST(e.bookingId AS string)) LIKE :keyword
                    OR LOWER(CAST(e.paymentId AS string)) LIKE :keyword
              )
            """)
    Page<PaymentEvent> search(
            @Param("bookingId") UUID bookingId,
            @Param("paymentId") UUID paymentId,
            @Param("eventType") PaymentEventType eventType,
            @Param("success") Boolean success,
            @Param("keyword") String keyword,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            Pageable pageable);

    @Query("""
            SELECT e
            FROM PaymentEvent e
            WHERE (:bookingId IS NULL OR e.bookingId = :bookingId)
              AND (:paymentId IS NULL OR e.paymentId = :paymentId)
              AND (:eventType IS NULL OR e.eventType = :eventType)
              AND (:success IS NULL OR e.success = :success)
              AND e.createdAt >= :fromTime
              AND e.createdAt < :toTime
              AND EXISTS (
                    SELECT b.id
                    FROM Booking b
                    WHERE b.id = e.bookingId
                      AND b.showtime.room.cinema.id IN :cinemaIds
              )
              AND (
                    :keyword IS NULL
                    OR LOWER(e.transactionNo) LIKE :keyword
                    OR LOWER(e.message) LIKE :keyword
                    OR LOWER(CAST(e.bookingId AS string)) LIKE :keyword
                    OR LOWER(CAST(e.paymentId AS string)) LIKE :keyword
              )
            """)
    Page<PaymentEvent> searchByCinemaIds(
            @Param("bookingId") UUID bookingId,
            @Param("paymentId") UUID paymentId,
            @Param("eventType") PaymentEventType eventType,
            @Param("success") Boolean success,
            @Param("keyword") String keyword,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            @Param("cinemaIds") List<UUID> cinemaIds,
            Pageable pageable);

    @Query("""
            SELECT e.eventType AS eventType,
                   e.method AS method,
                   e.success AS success,
                   COUNT(e) AS total
            FROM PaymentEvent e
            WHERE e.createdAt >= :fromTime
              AND e.createdAt < :toTime
            GROUP BY e.eventType, e.method, e.success
            """)
    List<PaymentEventSummaryRow> summarize(
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime);

    @Query("""
            SELECT e.eventType AS eventType,
                   e.method AS method,
                   e.success AS success,
                   COUNT(e) AS total
            FROM PaymentEvent e
            WHERE e.createdAt >= :fromTime
              AND e.createdAt < :toTime
              AND EXISTS (
                    SELECT b.id
                    FROM Booking b
                    WHERE b.id = e.bookingId
                      AND b.showtime.room.cinema.id IN :cinemaIds
              )
            GROUP BY e.eventType, e.method, e.success
            """)
    List<PaymentEventSummaryRow> summarizeByCinemaIds(
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            @Param("cinemaIds") List<UUID> cinemaIds);

    @Query("""
            SELECT COUNT(DISTINCT e.bookingId)
            FROM PaymentEvent e
            WHERE e.success = false
              AND e.bookingId IS NOT NULL
              AND e.createdAt >= :fromTime
              AND e.createdAt < :toTime
            """)
    long countDistinctFailedBookings(
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime);

    @Query("""
            SELECT COUNT(DISTINCT e.bookingId)
            FROM PaymentEvent e
            WHERE e.success = false
              AND e.bookingId IS NOT NULL
              AND e.createdAt >= :fromTime
              AND e.createdAt < :toTime
              AND EXISTS (
                    SELECT b.id
                    FROM Booking b
                    WHERE b.id = e.bookingId
                      AND b.showtime.room.cinema.id IN :cinemaIds
              )
            """)
    long countDistinctFailedBookingsByCinemaIds(
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            @Param("cinemaIds") List<UUID> cinemaIds);
}
