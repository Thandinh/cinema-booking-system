package com.cinema.booking.repository;

import com.cinema.booking.entity.Showtime;
import com.cinema.booking.enums.ShowtimeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Showtime s
            SET s.status = com.cinema.booking.enums.ShowtimeStatus.ENDED
            WHERE s.isDeleted = false
              AND s.status IN (
                  com.cinema.booking.enums.ShowtimeStatus.UPCOMING,
                  com.cinema.booking.enums.ShowtimeStatus.ONGOING
              )
              AND s.endTime <= :now
            """)
    int markFinishedShowtimesAsEnded(@Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Showtime s
            SET s.status = com.cinema.booking.enums.ShowtimeStatus.ONGOING
            WHERE s.isDeleted = false
              AND s.status = com.cinema.booking.enums.ShowtimeStatus.UPCOMING
              AND s.startTime <= :now
              AND s.endTime > :now
            """)
    int markStartedShowtimesAsOngoing(@Param("now") LocalDateTime now);

    // Sử dụng JOIN FETCH để lấy kèm Movie và Room (và Cinema bên trong Room), triệt tiêu hoàn toàn N+1 Query
    @Query("""
            SELECT s FROM Showtime s 
            JOIN FETCH s.movie 
            JOIN FETCH s.room r 
            JOIN FETCH r.cinema c
            WHERE s.id = :id
              AND s.isDeleted = false
              AND s.movie.isDeleted = false
              AND r.isDeleted = false
              AND c.isDeleted = false
            """)
    Optional<Showtime> findActiveById(@Param("id") UUID id);

    @Query(value = """
            SELECT s FROM Showtime s
            JOIN FETCH s.movie
            JOIN FETCH s.room r
            JOIN FETCH r.cinema c
            WHERE s.isDeleted = false
              AND s.movie.isDeleted = false
              AND r.isDeleted = false
              AND c.isDeleted = false
            """,
           countQuery = """
            SELECT COUNT(s) FROM Showtime s
            WHERE s.isDeleted = false
              AND s.movie.isDeleted = false
              AND s.room.isDeleted = false
              AND s.room.cinema.isDeleted = false
            """)
    Page<Showtime> findAllActive(Pageable pageable);

    @Query(value = """
            SELECT s FROM Showtime s
            JOIN FETCH s.movie
            JOIN FETCH s.room r
            JOIN FETCH r.cinema c
            WHERE s.isDeleted = false
              AND s.movie.isDeleted = false
              AND r.isDeleted = false
              AND c.isDeleted = false
              AND c.id IN :cinemaIds
            """,
           countQuery = """
            SELECT COUNT(s) FROM Showtime s
            WHERE s.isDeleted = false
              AND s.movie.isDeleted = false
              AND s.room.isDeleted = false
              AND s.room.cinema.isDeleted = false
              AND s.room.cinema.id IN :cinemaIds
            """)
    Page<Showtime> findAllActiveByCinemaIds(@Param("cinemaIds") List<UUID> cinemaIds, Pageable pageable);

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
            JOIN FETCH r.cinema c
            WHERE s.movie.id = :movieId
              AND s.isDeleted = false
              AND s.movie.isDeleted = false
              AND r.isDeleted = false
              AND c.isDeleted = false
            ORDER BY s.startTime ASC
            """)
    List<Showtime> findActiveByMovieId(@Param("movieId") UUID movieId);

    @Query("""
            SELECT s FROM Showtime s
            JOIN FETCH s.movie
            JOIN FETCH s.room r
            JOIN FETCH r.cinema c
            WHERE s.movie.id = :movieId
              AND s.isDeleted = false
              AND s.movie.isDeleted = false
              AND r.isDeleted = false
              AND c.isDeleted = false
              AND c.isActive = true
              AND s.status = com.cinema.booking.enums.ShowtimeStatus.UPCOMING
              AND s.startTime >= :fromTime
              AND s.startTime < :toTime
            ORDER BY s.startTime ASC
            """)
    List<Showtime> findBookableByMovieId(@Param("movieId") UUID movieId,
                                         @Param("fromTime") LocalDateTime fromTime,
                                         @Param("toTime") LocalDateTime toTime);
    
    // Lưu ý: Đối với Spring Data JPA Page, khi dùng JOIN FETCH bắt buộc phải viết kèm một query COUNT riêng biệt
    @Query(value = """
            SELECT s FROM Showtime s 
            JOIN FETCH s.movie 
            JOIN FETCH s.room r
            JOIN FETCH r.cinema c
            WHERE c.id = :cinemaId
              AND s.isDeleted = false
              AND s.movie.isDeleted = false
              AND r.isDeleted = false
              AND c.isDeleted = false
            """,
           countQuery = """
            SELECT COUNT(s) FROM Showtime s
            WHERE s.room.cinema.id = :cinemaId
              AND s.isDeleted = false
              AND s.movie.isDeleted = false
              AND s.room.isDeleted = false
              AND s.room.cinema.isDeleted = false
            """)
    Page<Showtime> findActiveByCinemaId(@Param("cinemaId") UUID cinemaId, Pageable pageable);

    @Query(value = """
            SELECT s FROM Showtime s
            JOIN FETCH s.movie
            JOIN FETCH s.room r
            JOIN FETCH r.cinema
            WHERE r.cinema.id = :cinemaId
              AND s.isDeleted = false
              AND s.movie.isDeleted = false
              AND r.isDeleted = false
              AND r.cinema.isDeleted = false
              AND r.cinema.isActive = true
              AND s.status = com.cinema.booking.enums.ShowtimeStatus.UPCOMING
              AND s.startTime >= :fromTime
              AND s.startTime < :toTime
            """,
           countQuery = """
            SELECT COUNT(s) FROM Showtime s
            WHERE s.room.cinema.id = :cinemaId
              AND s.isDeleted = false
              AND s.movie.isDeleted = false
              AND s.room.isDeleted = false
              AND s.room.cinema.isDeleted = false
              AND s.room.cinema.isActive = true
              AND s.status = com.cinema.booking.enums.ShowtimeStatus.UPCOMING
              AND s.startTime >= :fromTime
              AND s.startTime < :toTime
            """)
    Page<Showtime> findBookableByCinemaId(@Param("cinemaId") UUID cinemaId,
                                          @Param("fromTime") LocalDateTime fromTime,
                                          @Param("toTime") LocalDateTime toTime,
                                          Pageable pageable);

    @Query("""
            SELECT s FROM Showtime s
            JOIN FETCH s.movie
            JOIN FETCH s.room r
            JOIN FETCH r.cinema c
            WHERE r.cinema.id = :cinemaId
              AND s.isDeleted = false
              AND s.movie.isDeleted = false
              AND r.isDeleted = false
              AND c.isDeleted = false
              AND c.isActive = true
              AND s.status IN (
                  com.cinema.booking.enums.ShowtimeStatus.UPCOMING,
                  com.cinema.booking.enums.ShowtimeStatus.ONGOING
              )
              AND s.startTime >= :earliestStartTime
              AND s.startTime <= :latestStartTime
            ORDER BY s.startTime ASC, r.name ASC
            """)
    List<Showtime> findOpenForCheckIn(@Param("cinemaId") UUID cinemaId,
                                      @Param("earliestStartTime") LocalDateTime earliestStartTime,
                                      @Param("latestStartTime") LocalDateTime latestStartTime);

    @Query("""
            SELECT COUNT(s) > 0
            FROM Showtime s
            WHERE s.movie.id = :movieId
              AND s.isDeleted = false
              AND s.status IN :statuses
              AND s.endTime > :now
            """)
    boolean existsActiveScheduleByMovieId(@Param("movieId") UUID movieId,
                                          @Param("statuses") List<ShowtimeStatus> statuses,
                                          @Param("now") LocalDateTime now);

    @Query("""
            SELECT COUNT(s) > 0
            FROM Showtime s
            WHERE s.room.id = :roomId
              AND s.isDeleted = false
              AND s.status IN :statuses
              AND s.endTime > :now
            """)
    boolean existsActiveScheduleByRoomId(@Param("roomId") UUID roomId,
                                         @Param("statuses") List<ShowtimeStatus> statuses,
                                         @Param("now") LocalDateTime now);

    @Query("""
            SELECT COUNT(s) > 0
            FROM Showtime s
            WHERE s.room.cinema.id = :cinemaId
              AND s.isDeleted = false
              AND s.status IN :statuses
              AND s.endTime > :now
            """)
    boolean existsActiveScheduleByCinemaId(@Param("cinemaId") UUID cinemaId,
                                           @Param("statuses") List<ShowtimeStatus> statuses,
                                           @Param("now") LocalDateTime now);

    // ── Analytics ─────────────────────────────────────────────────────────────

    /** Tổng số suất chiếu active */
    Long countByIsDeletedFalse();

    @Query("""
            SELECT COUNT(s)
            FROM Showtime s
            WHERE s.isDeleted = false
              AND s.room.cinema.id IN :cinemaIds
            """)
    Long countByIsDeletedFalseAndCinemaIds(@Param("cinemaIds") List<UUID> cinemaIds);

    /** Đếm suất chiếu theo trạng thái */
    @Query("SELECT COUNT(s) FROM Showtime s WHERE s.status = com.cinema.booking.enums.ShowtimeStatus.UPCOMING AND s.isDeleted = false")
    Long countUpcomingShowtimes();

    @Query("""
            SELECT COUNT(s)
            FROM Showtime s
            WHERE s.status = com.cinema.booking.enums.ShowtimeStatus.UPCOMING
              AND s.isDeleted = false
              AND s.room.cinema.id IN :cinemaIds
            """)
    Long countUpcomingShowtimesByCinemaIds(@Param("cinemaIds") List<UUID> cinemaIds);
}
