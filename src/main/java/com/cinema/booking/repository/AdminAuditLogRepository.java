package com.cinema.booking.repository;

import com.cinema.booking.entity.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, UUID> {

    @Query("""
            SELECT l
            FROM AdminAuditLog l
            WHERE (:action IS NULL OR l.action = :action)
              AND (:resource IS NULL OR l.resource = :resource)
              AND (:success IS NULL OR l.success = :success)
              AND (
                    :keywordPattern IS NULL
                    OR LOWER(l.actorUsername) LIKE :keywordPattern
                    OR LOWER(l.requestPath) LIKE :keywordPattern
                    OR LOWER(l.resourceId) LIKE :keywordPattern
                    OR LOWER(l.ipAddress) LIKE :keywordPattern
              )
            """)
    Page<AdminAuditLog> search(
            @Param("action") String action,
            @Param("resource") String resource,
            @Param("success") Boolean success,
            @Param("keywordPattern") String keywordPattern,
            Pageable pageable);
}
