package com.cinema.booking.repository;

import com.cinema.booking.entity.Showtime;
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
public interface ShowtimeRepository extends JpaRepository<Showtime, UUID> {

    // Sử dụng JOIN FETCH để lấy kèm Movie và Room (và Cinema bên trong Room), triệt tiêu hoàn toàn N+1 Query
    @Query("""
            SELECT s FROM Showtime s 
            JOIN FETCH s.movie 
            JOIN FETCH s.room r 
            JOIN FETCH r.cinema
            WHERE s.id = :id AND s.isDeleted = false
            """)
    Optional<Showtime> findActiveById(@Param("id") UUID id);

    /**
     * Thuật toán Overlapping nâng cấp (15p dọn phòng).
     * Để tránh lỗi parse HQL, startTimeCheck và endTimeCheck (+/- 15p) sẽ được truyền trực tiếp từ Service.
     */
    @Query("""
            SELECT COUNT(s) > 0 FROM Showtime s
            WHERE s.room.id = :roomId
              AND s.isDeleted = false
              AND s.startTime < :endTimeCheck
              AND s.endTime > :startTimeCheck
            """)
    boolean isTimeOverlapping(@Param("roomId") UUID roomId,
                              @Param("startTimeCheck") LocalDateTime startTimeCheck,
                              @Param("endTimeCheck") LocalDateTime endTimeCheck);

    @Query("""
            SELECT COUNT(s) > 0 FROM Showtime s
            WHERE s.room.id = :roomId
              AND s.id != :excludeId
              AND s.isDeleted = false
              AND s.startTime < :endTimeCheck
              AND s.endTime > :startTimeCheck
            """)
    boolean isTimeOverlappingExclude(@Param("roomId") UUID roomId,
                                     @Param("startTimeCheck") LocalDateTime startTimeCheck,
                                     @Param("endTimeCheck") LocalDateTime endTimeCheck,
                                     @Param("excludeId") UUID excludeId);

    // Áp dụng JOIN FETCH cho tầng tìm kiếm danh sách để tăng tốc độ Render cho Frontend
    @Query("""
            SELECT s FROM Showtime s 
            JOIN FETCH s.movie 
            JOIN FETCH s.room r
            JOIN FETCH r.cinema
            WHERE s.movie.id = :movieId AND s.isDeleted = false 
            ORDER BY s.startTime ASC
            """)
    List<Showtime> findActiveByMovieId(@Param("movieId") UUID movieId);
    
    // Lưu ý: Đối với Spring Data JPA Page, khi dùng JOIN FETCH bắt buộc phải viết kèm một query COUNT riêng biệt
    @Query(value = """
            SELECT s FROM Showtime s 
            JOIN FETCH s.movie 
            JOIN FETCH s.room r
            JOIN FETCH r.cinema
            WHERE r.cinema.id = :cinemaId AND s.isDeleted = false
            """,
           countQuery = "SELECT COUNT(s) FROM Showtime s WHERE s.room.cinema.id = :cinemaId AND s.isDeleted = false")
    Page<Showtime> findActiveByCinemaId(@Param("cinemaId") UUID cinemaId, Pageable pageable);
}
