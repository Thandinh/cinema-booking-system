package com.cinema.booking.repository;

import com.cinema.booking.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    List<Room> findAllByCinemaIdAndIsDeletedFalse(UUID cinemaId);

    @Query("SELECT r FROM Room r WHERE r.id = :id AND r.isDeleted = false")
    Optional<Room> findActiveById(@Param("id") UUID id);

    boolean existsByCinemaIdAndNameAndIsDeletedFalse(UUID cinemaId, String name);

    /** Soft-delete toàn bộ phòng của một rạp — dùng khi xóa mềm rạp (cascade) */
    @Modifying
    @Query("UPDATE Room r SET r.isDeleted = true WHERE r.cinema.id = :cinemaId AND r.isDeleted = false")
    void softDeleteByCinemaId(@Param("cinemaId") UUID cinemaId);

    /** Lấy ID tất cả phòng chưa xóa của rạp — dùng để cascade xóa ghế */
    @Query("SELECT r.id FROM Room r WHERE r.cinema.id = :cinemaId AND r.isDeleted = false")
    List<UUID> findActiveRoomIdsByCinemaId(@Param("cinemaId") UUID cinemaId);
}
