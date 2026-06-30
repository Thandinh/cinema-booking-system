package com.cinema.booking.repository;

import com.cinema.booking.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /** Lấy danh sách user chưa bị xoá mềm, hỗ trợ phân trang */
    @Query("SELECT u FROM User u WHERE u.isDeleted = false OR u.isDeleted IS NULL")
    Page<User> findAllByIsDeletedFalse(Pageable pageable);

    /** Tìm user chưa bị xoá mềm theo UUID */
    @Query("SELECT u FROM User u WHERE u.id = :id AND (u.isDeleted = false OR u.isDeleted IS NULL)")
    Optional<User> findActiveById(@Param("id") UUID id);

    // ── Analytics ─────────────────────────────────────────────────────────────

    /** Đếm user đăng ký trong ngày */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startOfDay AND u.createdAt < :endOfDay AND (u.isDeleted = false OR u.isDeleted IS NULL)")
    Long countNewUsersToday(@Param("startOfDay") LocalDateTime startOfDay,
                            @Param("endOfDay")   LocalDateTime endOfDay);

    /** Đếm user đăng ký trong tháng */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startOfMonth AND u.createdAt < :endOfMonth AND (u.isDeleted = false OR u.isDeleted IS NULL)")
    Long countNewUsersThisMonth(@Param("startOfMonth") LocalDateTime startOfMonth,
                                @Param("endOfMonth")   LocalDateTime endOfMonth);

    /** Tổng số user active */
    @Query("SELECT COUNT(u) FROM User u WHERE (u.isDeleted = false OR u.isDeleted IS NULL)")
    Long countActiveUsers();
}
