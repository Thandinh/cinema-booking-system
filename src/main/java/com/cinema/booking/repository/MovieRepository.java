package com.cinema.booking.repository;

import com.cinema.booking.entity.Movie;
import com.cinema.booking.enums.MovieStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MovieRepository extends JpaRepository<Movie, UUID> {

    /** Lấy danh sách phim chưa bị xoá mềm, hỗ trợ phân trang */
    Page<Movie> findAllByIsDeletedFalse(Pageable pageable);

    /** Lấy danh sách phim theo trạng thái (chưa bị xoá mềm) */
    Page<Movie> findAllByStatusAndIsDeletedFalse(MovieStatus status, Pageable pageable);

    @Query(
            value = """
                    SELECT m.*
                    FROM movies m
                    LEFT JOIN showtimes st
                           ON st.movie_id = m.id
                          AND st.is_deleted = false
                    LEFT JOIN bookings b
                           ON b.showtime_id = st.id
                          AND b.status = 'SUCCESS'
                    LEFT JOIN booking_details bd
                           ON bd.booking_id = b.id
                    WHERE m.status = :status
                      AND m.is_deleted = false
                    GROUP BY m.id
                    ORDER BY
                        COUNT(DISTINCT b.id) DESC,
                        COUNT(bd.id) DESC,
                        m.rating_imdb DESC NULLS LAST,
                        m.release_date DESC NULLS LAST,
                        m.created_at DESC NULLS LAST
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM movies m
                    WHERE m.status = :status
                      AND m.is_deleted = false
                    """,
            nativeQuery = true
    )
    Page<Movie> findByStatusOrderByPopularity(@Param("status") String status, Pageable pageable);

    @Query(
            value = """
                    SELECT m.*
                    FROM movies m
                    WHERE m.status = :status
                      AND m.is_deleted = false
                    ORDER BY
                        m.release_date ASC NULLS LAST,
                        m.rating_imdb DESC NULLS LAST,
                        m.created_at DESC NULLS LAST
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM movies m
                    WHERE m.status = :status
                      AND m.is_deleted = false
                    """,
            nativeQuery = true
    )
    Page<Movie> findByStatusOrderByReleaseDateAsc(@Param("status") String status, Pageable pageable);

    /** Tìm phim chưa bị xoá mềm theo ID */
    @Query("SELECT m FROM Movie m WHERE m.id = :id AND m.isDeleted = false")
    Optional<Movie> findActiveById(UUID id);

    /** Kiểm tra trùng tên phim */
    boolean existsByTitleAndIsDeletedFalse(String title);

    // ── Analytics ─────────────────────────────────────────────────────────────

    /** Đếm phim theo trạng thái */
    Long countByStatusAndIsDeletedFalse(MovieStatus status);

    /** Tổng số phim active */
    Long countByIsDeletedFalse();
}
