package com.cinema.booking.repository;

import com.cinema.booking.entity.Promotion;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

    @Query("SELECT p FROM Promotion p WHERE p.id = :id AND p.isDeleted = false")
    Optional<Promotion> findActiveById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Promotion p WHERE p.id = :id")
    Optional<Promotion> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT p FROM Promotion p WHERE p.code = :code AND p.isDeleted = false")
    Optional<Promotion> findActiveByCode(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Promotion p WHERE p.code = :code AND p.isDeleted = false")
    Optional<Promotion> findActiveByCodeForUpdate(@Param("code") String code);

    @Modifying
    @Query(value = """
            UPDATE promotions p
            SET used_count = GREATEST(0, p.used_count - reservations.reserved_count),
                updated_at = CURRENT_TIMESTAMP
            FROM (
                SELECT promotion_id, COUNT(*) AS reserved_count
                FROM bookings
                WHERE id IN (:bookingIds)
                  AND promotion_reserved = true
                  AND promotion_id IS NOT NULL
                GROUP BY promotion_id
            ) reservations
            WHERE p.id = reservations.promotion_id
            """, nativeQuery = true)
    int releaseReservationsForBookingIds(@Param("bookingIds") List<UUID> bookingIds);

    boolean existsByCodeAndIsDeletedFalse(String code);

    @Query("SELECT p FROM Promotion p WHERE p.isDeleted = false")
    Page<Promotion> findAllActive(Pageable pageable);

    @Query("""
            SELECT p FROM Promotion p
            WHERE p.isDeleted = false
              AND (:keywordPattern IS NULL
                   OR LOWER(p.code) LIKE :keywordPattern
                   OR LOWER(p.description) LIKE :keywordPattern)
              AND (:active IS NULL OR p.isActive = :active)
              AND (:availableOnly = false OR (
                    p.isActive = true
                    AND p.startDate <= :now
                    AND p.endDate >= :now
                    AND (p.usageLimit IS NULL OR p.usedCount < p.usageLimit)
              ))
              AND (:upcomingOnly = false OR p.startDate > :now)
              AND (:expiredOnly = false OR p.endDate < :now)
              AND (:exhaustedOnly = false OR (
                    p.usageLimit IS NOT NULL
                    AND p.usedCount >= p.usageLimit
              ))
            """)
    Page<Promotion> searchAdminPromotions(
            @Param("keywordPattern") String keywordPattern,
            @Param("active") Boolean active,
            @Param("availableOnly") boolean availableOnly,
            @Param("upcomingOnly") boolean upcomingOnly,
            @Param("expiredOnly") boolean expiredOnly,
            @Param("exhaustedOnly") boolean exhaustedOnly,
            @Param("now") LocalDateTime now,
            Pageable pageable);
    
    // Tìm các khuyến mãi đang có hiệu lực (active, trong thời gian, chưa hết lượt)
    @Query("""
            SELECT p FROM Promotion p 
            WHERE p.isDeleted = false 
              AND p.isActive = true
              AND p.startDate <= :now 
              AND p.endDate >= :now
              AND (p.usageLimit IS NULL OR p.usedCount < p.usageLimit)
            """)
    Page<Promotion> findAvailablePromotions(@Param("now") LocalDateTime now, Pageable pageable);
}
