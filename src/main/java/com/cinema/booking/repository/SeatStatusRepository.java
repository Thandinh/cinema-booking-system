package com.cinema.booking.repository;

import com.cinema.booking.entity.SeatStatus;
import com.cinema.booking.enums.SeatStatusType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SeatStatusRepository extends JpaRepository<SeatStatus, UUID> {

    @Query("""
            SELECT ss FROM SeatStatus ss
            JOIN FETCH ss.seat s
            JOIN FETCH ss.showtime
            WHERE ss.showtime.id = :showtimeId
            ORDER BY s.rowIndex ASC, s.colIndex ASC, s.rowLabel ASC, s.seatNumber ASC
            """)
    List<SeatStatus> findAllByShowtimeId(@Param("showtimeId") UUID showtimeId);

    // ===================================================================================
    // ANTI RACE-CONDITION: Dùng Pessimistic Lock khi chọn ghế để tránh 2 người lấy 1 ghế
    // ===================================================================================
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ss FROM SeatStatus ss JOIN FETCH ss.seat s WHERE ss.showtime.id = :showtimeId AND s.id IN :seatIds")
    List<SeatStatus> findForUpdateByShowtimeAndSeats(@Param("showtimeId") UUID showtimeId, @Param("seatIds") List<UUID> seatIds);

    // ===================================================================================
    // BULK UPDATE: Cập nhật nhiều ghế cùng lúc cho hiệu năng cao
    // ===================================================================================
    @Modifying
    @Query("UPDATE SeatStatus ss SET ss.status = :status WHERE ss.showtime.id = :showtimeId AND ss.seat.id IN :seatIds")
    void bulkUpdateStatus(@Param("showtimeId") UUID showtimeId, @Param("seatIds") List<UUID> seatIds, @Param("status") SeatStatusType status);

    /**
     * QUAN TRỌNG: Query này bypass @Version (Optimistic Lock).
     * Điều này là có chủ ý: khi BOOKED/AVAILABLE, race condition không còn là rủi ro
     * vì ghế đã được hold bởi PESSIMISTIC_WRITE ở bước trước.
     */
    @Modifying
    @Query("UPDATE SeatStatus ss SET ss.status = :status, ss.holdBy = null, ss.holdUntil = null WHERE ss.showtime.id = :showtimeId AND ss.seat.id IN :seatIds")
    void bulkUpdateStatusAndClearHold(@Param("showtimeId") UUID showtimeId, @Param("seatIds") List<UUID> seatIds, @Param("status") SeatStatusType status);

    @Modifying
    @Query("""
            UPDATE SeatStatus ss
            SET ss.status = :status, ss.holdBy = null, ss.holdUntil = null
            WHERE ss.showtime.id = :showtimeId
              AND ss.seat.id IN :seatIds
              AND ss.status = com.cinema.booking.enums.SeatStatusType.HOLD
              AND ss.holdBy.id = :userId
              AND ss.holdUntil <= :holdUntilBeforeOrAt
            """)
    int releaseHeldSeatsForBooking(
            @Param("showtimeId") UUID showtimeId,
            @Param("seatIds") List<UUID> seatIds,
            @Param("userId") UUID userId,
            @Param("holdUntilBeforeOrAt") LocalDateTime holdUntilBeforeOrAt,
            @Param("status") SeatStatusType status);

    // ===================================================================================
    // SCHEDULER: Tìm ghế đã quá thời gian HOLD
    // ===================================================================================
    @Query(value = """
            SELECT ss.id AS "id",
                   ss.showtime_id AS "showtimeId",
                   ss.seat_id AS "seatId"
            FROM seat_status ss
            WHERE ss.status = 'HOLD'
              AND ss.hold_until <= :now
            ORDER BY ss.hold_until ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<ExpiredSeatHoldProjection> findExpiredHoldRows(
            @Param("now") LocalDateTime now,
            @Param("limit") int limit);

    @Query(value = """
            SELECT ss.id AS "id",
                   ss.showtime_id AS "showtimeId",
                   ss.seat_id AS "seatId"
            FROM seat_status ss
            WHERE ss.showtime_id = :showtimeId
              AND ss.status = 'HOLD'
              AND ss.hold_until <= :now
            ORDER BY ss.hold_until ASC
            """, nativeQuery = true)
    List<ExpiredSeatHoldProjection> findExpiredHoldRowsByShowtime(
            @Param("showtimeId") UUID showtimeId,
            @Param("now") LocalDateTime now);

    @Modifying
    @Query(value = """
            UPDATE seat_status
            SET status = 'AVAILABLE',
                hold_by = NULL,
                hold_until = NULL
            WHERE id IN (:ids)
              AND status = 'HOLD'
            """, nativeQuery = true)
    int releaseExpiredHoldsByIds(@Param("ids") List<UUID> ids);

    @Modifying
    @Query("DELETE FROM SeatStatus ss WHERE ss.showtime.id = :showtimeId")
    void deleteByShowtimeId(@Param("showtimeId") UUID showtimeId);

    boolean existsBySeatIdAndStatusIn(UUID seatId, List<SeatStatusType> statuses);
}
