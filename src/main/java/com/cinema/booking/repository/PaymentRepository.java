package com.cinema.booking.repository;

import com.cinema.booking.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @Query("SELECT p FROM Payment p JOIN FETCH p.booking b WHERE b.user.id = :userId")
    Page<Payment> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT p FROM Payment p JOIN FETCH p.booking")
    Page<Payment> findAllWithDetails(Pageable pageable);
}
