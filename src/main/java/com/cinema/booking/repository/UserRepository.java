package com.cinema.booking.repository;

import com.cinema.booking.entity.User;
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
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<User> findByEmailIgnoreCase(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    boolean existsByEmailIgnoreCase(@Param("email") String email);

    Optional<User> findByEmailVerificationTokenHash(String emailVerificationTokenHash);

    Optional<User> findByPasswordResetTokenHash(String passwordResetTokenHash);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /** Lấy danh sách user chưa bị xoá mềm, hỗ trợ phân trang */
    @Query("SELECT u FROM User u WHERE u.isDeleted = false OR u.isDeleted IS NULL")
    Page<User> findAllByIsDeletedFalse(Pageable pageable);

    @Query(value = "SELECT u.id FROM User u WHERE u.isDeleted = false OR u.isDeleted IS NULL",
           countQuery = "SELECT COUNT(u) FROM User u WHERE u.isDeleted = false OR u.isDeleted IS NULL")
    Page<UUID> findActiveIds(Pageable pageable);

    @Query(value = """
            SELECT u.id
            FROM User u
            WHERE (u.isDeleted = false OR u.isDeleted IS NULL)
              AND (
                    :roleName IS NULL
                    OR EXISTS (
                        SELECT r.id
                        FROM u.roles r
                        WHERE r.name = :roleName
                    )
                  )
              AND (
                    :assignedCity IS NULL
                    OR EXISTS (
                        SELECT scCity.id
                        FROM StaffCinema scCity
                        JOIN scCity.cinema cCity
                        WHERE scCity.staff = u
                          AND LOWER(cCity.city) = :assignedCity
                          AND cCity.isDeleted = false
                          AND cCity.isActive = true
                    )
                  )
              AND (
                    :assignedCinemaId IS NULL
                    OR EXISTS (
                        SELECT sc.id
                        FROM StaffCinema sc
                        JOIN sc.cinema c
                        WHERE sc.staff = u
                          AND c.id = :assignedCinemaId
                          AND c.isDeleted = false
                          AND c.isActive = true
                    )
                  )
              AND (
                    :unassignedStaff = false
                    OR NOT EXISTS (
                        SELECT sc2.id
                        FROM StaffCinema sc2
                        JOIN sc2.cinema c2
                        WHERE sc2.staff = u
                          AND c2.isDeleted = false
                          AND c2.isActive = true
                    )
                  )
              AND (
                    :keywordPattern IS NULL
                    OR LOWER(u.username) LIKE :keywordPattern
                    OR (u.email IS NOT NULL AND LOWER(u.email) LIKE :keywordPattern)
                    OR (u.firstName IS NOT NULL AND LOWER(u.firstName) LIKE :keywordPattern)
                    OR (u.lastName IS NOT NULL AND LOWER(u.lastName) LIKE :keywordPattern)
                    OR (u.phone IS NOT NULL AND LOWER(u.phone) LIKE :keywordPattern)
                  )
            """,
           countQuery = """
            SELECT COUNT(u.id)
            FROM User u
            WHERE (u.isDeleted = false OR u.isDeleted IS NULL)
              AND (
                    :roleName IS NULL
                    OR EXISTS (
                        SELECT r.id
                        FROM u.roles r
                        WHERE r.name = :roleName
                    )
                  )
              AND (
                    :assignedCity IS NULL
                    OR EXISTS (
                        SELECT scCity.id
                        FROM StaffCinema scCity
                        JOIN scCity.cinema cCity
                        WHERE scCity.staff = u
                          AND LOWER(cCity.city) = :assignedCity
                          AND cCity.isDeleted = false
                          AND cCity.isActive = true
                    )
                  )
              AND (
                    :assignedCinemaId IS NULL
                    OR EXISTS (
                        SELECT sc.id
                        FROM StaffCinema sc
                        JOIN sc.cinema c
                        WHERE sc.staff = u
                          AND c.id = :assignedCinemaId
                          AND c.isDeleted = false
                          AND c.isActive = true
                    )
                  )
              AND (
                    :unassignedStaff = false
                    OR NOT EXISTS (
                        SELECT sc2.id
                        FROM StaffCinema sc2
                        JOIN sc2.cinema c2
                        WHERE sc2.staff = u
                          AND c2.isDeleted = false
                          AND c2.isActive = true
                    )
                  )
              AND (
                    :keywordPattern IS NULL
                    OR LOWER(u.username) LIKE :keywordPattern
                    OR (u.email IS NOT NULL AND LOWER(u.email) LIKE :keywordPattern)
                    OR (u.firstName IS NOT NULL AND LOWER(u.firstName) LIKE :keywordPattern)
                    OR (u.lastName IS NOT NULL AND LOWER(u.lastName) LIKE :keywordPattern)
                    OR (u.phone IS NOT NULL AND LOWER(u.phone) LIKE :keywordPattern)
                  )
            """)
    Page<UUID> findActiveIdsByRoleKeywordAndStaffCinema(@Param("roleName") String roleName,
                                                        @Param("keywordPattern") String keywordPattern,
                                                        @Param("assignedCity") String assignedCity,
                                                        @Param("assignedCinemaId") UUID assignedCinemaId,
                                                        @Param("unassignedStaff") boolean unassignedStaff,
                                                        Pageable pageable);

    @Query("""
            SELECT DISTINCT u FROM User u
            LEFT JOIN FETCH u.roles r
            LEFT JOIN FETCH r.permissions
            WHERE u.id IN :ids
            """)
    List<User> findAllWithRolesByIdIn(@Param("ids") List<UUID> ids);

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
