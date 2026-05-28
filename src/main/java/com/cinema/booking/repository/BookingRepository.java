package com.cinema.booking.repository;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findBySecureToken(String secureToken);

    @Query("SELECT b FROM Booking b JOIN FETCH b.showtime s JOIN FETCH s.movie JOIN FETCH s.room r JOIN FETCH r.cinema WHERE b.user.id = :userId")
    Page<Booking> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT b FROM Booking b JOIN FETCH b.showtime s JOIN FETCH s.movie JOIN FETCH s.room r JOIN FETCH r.cinema WHERE (:status IS NULL OR b.status = :status)")
    Page<Booking> findAllByStatus(@Param("status") BookingStatus status, Pageable pageable);

    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.bookingDetails bd LEFT JOIN FETCH bd.seat WHERE b.id = :id")
    Optional<Booking> findWithDetailsById(@Param("id") UUID id);
}
