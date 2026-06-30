package com.cinema.booking.repository;

import com.cinema.booking.entity.Movie;
import com.cinema.booking.enums.MovieStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MovieRepository extends JpaRepository<Movie, UUID> {

    /** Lấy danh sách phim chưa bị xoá mềm, hỗ trợ phân trang */
    Page<Movie> findAllByIsDeletedFalse(Pageable pageable);

    /** Lấy danh sách phim theo trạng thái (chưa bị xoá mềm) */
    Page<Movie> findAllByStatusAndIsDeletedFalse(MovieStatus status, Pageable pageable);

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
