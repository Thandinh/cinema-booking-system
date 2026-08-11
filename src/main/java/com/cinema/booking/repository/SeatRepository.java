package com.cinema.booking.repository;

import com.cinema.booking.configuration.CacheConfig;
import com.cinema.booking.entity.Seat;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface SeatRepository extends JpaRepository<Seat, UUID> {

    /** Lấy toàn bộ ghế chưa xoá của một phòng, sắp xếp theo hàng rồi số ghế */
    @Query("""
            SELECT s FROM Seat s
            JOIN FETCH s.room
            WHERE s.room.id = :roomId AND s.isDeleted = false
            ORDER BY s.rowLabel ASC, s.seatNumber ASC
            """)
    @Cacheable(cacheNames = CacheConfig.SEATS_BY_ROOM, key = "#roomId")
    List<Seat> findActiveByRoomId(@Param("roomId") UUID roomId);

    /** Tìm ghế chưa xoá theo ID */
    @Query("SELECT s FROM Seat s WHERE s.id = :id AND s.isDeleted = false")
    Optional<Seat> findActiveById(@Param("id") UUID id);

    /** Kiểm tra trùng ghế theo room + row + number (bất kể is_deleted) */
    boolean existsByRoomIdAndRowLabelAndSeatNumber(UUID roomId, String rowLabel, Integer seatNumber);

    /**
     * Load toàn bộ seat key ("rowLabel:seatNumber") của một phòng trong 1 query duy nhất.
     * Dùng trong bulk-generate để tránh N+1 EXISTS queries.
     */
    @Query("SELECT CONCAT(s.rowLabel, ':', s.seatNumber) FROM Seat s WHERE s.room.id = :roomId")
    Set<String> findSeatKeysByRoomId(@Param("roomId") UUID roomId);

    /** Đếm ghế còn hoạt động trong phòng */
    @Query("SELECT COUNT(s) FROM Seat s WHERE s.room.id = :roomId AND s.isDeleted = false")
    long countActiveByRoomId(@Param("roomId") UUID roomId);

    /** Soft-delete toàn bộ ghế của một phòng — dùng khi xóa mềm phòng chiếu (cascade) */
    @Modifying
    @Query("UPDATE Seat s SET s.isDeleted = true WHERE s.room.id = :roomId AND s.isDeleted = false")
    void softDeleteByRoomId(@Param("roomId") UUID roomId);

    @Modifying
    @Query("UPDATE Seat s SET s.isDeleted = true WHERE s.room.cinema.id = :cinemaId AND s.isDeleted = false")
    int softDeleteByCinemaId(@Param("cinemaId") UUID cinemaId);
}
