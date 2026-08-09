package com.cinema.booking.repository;

import com.cinema.booking.entity.Refund;
import com.cinema.booking.enums.PaymentMethod;
import com.cinema.booking.enums.RefundStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundRepository extends JpaRepository<Refund, UUID> {

    Optional<Refund> findFirstByPayment_IdAndStatusIn(UUID paymentId, List<RefundStatus> statuses);

    @Query(value = """
            SELECT rf
            FROM Refund rf
            JOIN FETCH rf.booking b
            JOIN FETCH b.user u
            JOIN FETCH b.showtime st
            JOIN FETCH st.movie m
            JOIN FETCH st.room r
            JOIN FETCH r.cinema c
            JOIN FETCH rf.payment p
            WHERE (:status IS NULL OR rf.status = :status)
              AND (:method IS NULL OR rf.method = :method)
              AND (:cinemaId IS NULL OR c.id = :cinemaId)
              AND (:city IS NULL OR c.city = :city)
              AND rf.createdAt >= :fromTime
              AND rf.createdAt < :toTime
              AND (
                    :keywordPattern IS NULL
                    OR LOWER(CAST(rf.id AS string)) LIKE :keywordPattern
                    OR LOWER(CAST(b.id AS string)) LIKE :keywordPattern
                    OR LOWER(p.transactionNo) LIKE :keywordPattern
                    OR LOWER(u.username) LIKE :keywordPattern
                    OR LOWER(u.email) LIKE :keywordPattern
                    OR LOWER(m.title) LIKE :keywordPattern
                    OR LOWER(c.name) LIKE :keywordPattern
              )
            """,
           countQuery = """
            SELECT COUNT(rf)
            FROM Refund rf
            JOIN rf.booking b
            JOIN b.user u
            JOIN b.showtime st
            JOIN st.movie m
            JOIN st.room r
            JOIN r.cinema c
            JOIN rf.payment p
            WHERE (:status IS NULL OR rf.status = :status)
              AND (:method IS NULL OR rf.method = :method)
              AND (:cinemaId IS NULL OR c.id = :cinemaId)
              AND (:city IS NULL OR c.city = :city)
              AND rf.createdAt >= :fromTime
              AND rf.createdAt < :toTime
              AND (
                    :keywordPattern IS NULL
                    OR LOWER(CAST(rf.id AS string)) LIKE :keywordPattern
                    OR LOWER(CAST(b.id AS string)) LIKE :keywordPattern
                    OR LOWER(p.transactionNo) LIKE :keywordPattern
                    OR LOWER(u.username) LIKE :keywordPattern
                    OR LOWER(u.email) LIKE :keywordPattern
                    OR LOWER(m.title) LIKE :keywordPattern
                    OR LOWER(c.name) LIKE :keywordPattern
              )
            """)
    Page<Refund> search(
            @Param("status") RefundStatus status,
            @Param("method") PaymentMethod method,
            @Param("keywordPattern") String keywordPattern,
            @Param("cinemaId") UUID cinemaId,
            @Param("city") String city,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            Pageable pageable);

    @Query(value = """
            SELECT rf
            FROM Refund rf
            JOIN FETCH rf.booking b
            JOIN FETCH b.user u
            JOIN FETCH b.showtime st
            JOIN FETCH st.movie m
            JOIN FETCH st.room r
            JOIN FETCH r.cinema c
            JOIN FETCH rf.payment p
            WHERE (:status IS NULL OR rf.status = :status)
              AND (:method IS NULL OR rf.method = :method)
              AND c.id IN :cinemaIds
              AND (:cinemaId IS NULL OR c.id = :cinemaId)
              AND (:city IS NULL OR c.city = :city)
              AND rf.createdAt >= :fromTime
              AND rf.createdAt < :toTime
              AND (
                    :keywordPattern IS NULL
                    OR LOWER(CAST(rf.id AS string)) LIKE :keywordPattern
                    OR LOWER(CAST(b.id AS string)) LIKE :keywordPattern
                    OR LOWER(p.transactionNo) LIKE :keywordPattern
                    OR LOWER(u.username) LIKE :keywordPattern
                    OR LOWER(u.email) LIKE :keywordPattern
                    OR LOWER(m.title) LIKE :keywordPattern
                    OR LOWER(c.name) LIKE :keywordPattern
              )
            """,
           countQuery = """
            SELECT COUNT(rf)
            FROM Refund rf
            JOIN rf.booking b
            JOIN b.user u
            JOIN b.showtime st
            JOIN st.movie m
            JOIN st.room r
            JOIN r.cinema c
            JOIN rf.payment p
            WHERE (:status IS NULL OR rf.status = :status)
              AND (:method IS NULL OR rf.method = :method)
              AND c.id IN :cinemaIds
              AND (:cinemaId IS NULL OR c.id = :cinemaId)
              AND (:city IS NULL OR c.city = :city)
              AND rf.createdAt >= :fromTime
              AND rf.createdAt < :toTime
              AND (
                    :keywordPattern IS NULL
                    OR LOWER(CAST(rf.id AS string)) LIKE :keywordPattern
                    OR LOWER(CAST(b.id AS string)) LIKE :keywordPattern
                    OR LOWER(p.transactionNo) LIKE :keywordPattern
                    OR LOWER(u.username) LIKE :keywordPattern
                    OR LOWER(u.email) LIKE :keywordPattern
                    OR LOWER(m.title) LIKE :keywordPattern
                    OR LOWER(c.name) LIKE :keywordPattern
              )
            """)
    Page<Refund> searchByCinemaIds(
            @Param("status") RefundStatus status,
            @Param("method") PaymentMethod method,
            @Param("keywordPattern") String keywordPattern,
            @Param("cinemaId") UUID cinemaId,
            @Param("city") String city,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            @Param("cinemaIds") List<UUID> cinemaIds,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT rf
            FROM Refund rf
            JOIN FETCH rf.booking b
            JOIN FETCH b.user
            JOIN FETCH b.showtime st
            JOIN FETCH st.movie
            JOIN FETCH st.room r
            JOIN FETCH r.cinema
            JOIN FETCH rf.payment
            WHERE rf.id = :id
            """)
    Optional<Refund> findLockedWithDetailsById(@Param("id") UUID id);
}
