package com.cinema.booking.repository;

import com.cinema.booking.entity.AuthAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLog, UUID> {

    @Query("""
            select log
            from AuthAuditLog log
            where (:eventType is null or log.eventType = :eventType)
              and (:success is null or log.success = :success)
              and (
                    :keyword is null
                    or lower(log.username) like :keyword
                    or lower(log.ipAddress) like :keyword
                    or lower(log.userAgent) like :keyword
                    or lower(log.failureReason) like :keyword
                  )
            order by log.createdAt desc
            """)
    Page<AuthAuditLog> search(
            @Param("eventType") String eventType,
            @Param("success") Boolean success,
            @Param("keyword") String keyword,
            Pageable pageable);
}
