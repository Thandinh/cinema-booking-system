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

    @Query("SELECT ss FROM SeatStatus ss JOIN FETCH ss.seat WHERE ss.showtime.id = :showtimeId")
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

    // ===================================================================================
    // SCHEDULER: Tìm ghế đã quá thời gian HOLD
    // ===================================================================================
    @Query("SELECT ss FROM SeatStatus ss JOIN FETCH ss.seat JOIN FETCH ss.showtime WHERE ss.status = :status AND ss.holdUntil < :now")
    List<SeatStatus> findExpiredHolds(@Param("status") SeatStatusType status, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM SeatStatus ss WHERE ss.showtime.id = :showtimeId")
    void deleteByShowtimeId(@Param("showtimeId") UUID showtimeId);

    boolean existsBySeatIdAndStatusIn(UUID seatId, List<SeatStatusType> statuses);
}
