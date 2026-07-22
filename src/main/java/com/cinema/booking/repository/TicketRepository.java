package com.cinema.booking.repository;

import com.cinema.booking.entity.Ticket;
import com.cinema.booking.enums.TicketStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Optional<Ticket> findByQrCode(String qrCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("""
            SELECT t FROM Ticket t
            JOIN FETCH t.bookingDetail bd
            JOIN FETCH bd.booking b
            JOIN FETCH b.showtime s
            JOIN FETCH s.movie
            JOIN FETCH s.room r
            JOIN FETCH r.cinema
            JOIN FETCH bd.seat
            LEFT JOIN FETCH t.checkedInBy
            WHERE t.qrCode = :qrCode
            """)
    Optional<Ticket> findByQrCodeForCheckIn(@Param("qrCode") String qrCode);

    @Query(value = "SELECT t FROM Ticket t JOIN FETCH t.bookingDetail bd JOIN FETCH bd.seat JOIN FETCH bd.booking b JOIN FETCH b.showtime s JOIN FETCH s.movie JOIN FETCH s.room r JOIN FETCH r.cinema LEFT JOIN FETCH t.checkedInBy WHERE b.user.id = :userId",
           countQuery = "SELECT COUNT(t) FROM Ticket t WHERE t.bookingDetail.booking.user.id = :userId")
    Page<Ticket> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query(value = "SELECT t FROM Ticket t JOIN FETCH t.bookingDetail bd JOIN FETCH bd.seat JOIN FETCH bd.booking b JOIN FETCH b.showtime s JOIN FETCH s.movie JOIN FETCH s.room r JOIN FETCH r.cinema LEFT JOIN FETCH t.checkedInBy",
           countQuery = "SELECT COUNT(t) FROM Ticket t")
    Page<Ticket> findAllWithDetails(Pageable pageable);

    // ── Analytics ─────────────────────────────────────────────────────────────

    /** Tổng số vé theo trạng thái */
    Long countByStatus(TicketStatus status);
}
