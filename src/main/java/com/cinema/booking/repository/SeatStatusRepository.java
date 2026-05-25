package com.cinema.booking.repository;

import com.cinema.booking.entity.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SeatStatusRepository extends JpaRepository<SeatStatus, UUID> {

    @Modifying
    @Query("DELETE FROM SeatStatus ss WHERE ss.showtime.id = :showtimeId")
    void deleteByShowtimeId(@Param("showtimeId") UUID showtimeId);

    // TODO: sẽ bổ sung thêm các method liên quan đến booking/hold ghế sau
}
