package com.cinema.booking.repository;

import com.cinema.booking.entity.PaymentEvent;
import com.cinema.booking.enums.PaymentEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, UUID> {

    @Query("""
            SELECT e
            FROM PaymentEvent e
            WHERE (:bookingId IS NULL OR e.bookingId = :bookingId)
              AND (:paymentId IS NULL OR e.paymentId = :paymentId)
              AND (:eventType IS NULL OR e.eventType = :eventType)
              AND (:success IS NULL OR e.success = :success)
              AND (
                    :keyword IS NULL
                    OR LOWER(e.transactionNo) LIKE :keyword
                    OR LOWER(e.message) LIKE :keyword
                    OR LOWER(CAST(e.bookingId AS string)) LIKE :keyword
                    OR LOWER(CAST(e.paymentId AS string)) LIKE :keyword
              )
            """)
    Page<PaymentEvent> search(
            @Param("bookingId") UUID bookingId,
            @Param("paymentId") UUID paymentId,
            @Param("eventType") PaymentEventType eventType,
            @Param("success") Boolean success,
            @Param("keyword") String keyword,
            Pageable pageable);
}
