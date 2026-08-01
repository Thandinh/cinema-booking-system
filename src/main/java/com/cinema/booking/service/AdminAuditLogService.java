package com.cinema.booking.service;

import com.cinema.booking.dto.request.AuditLogSearchRequest;
import com.cinema.booking.dto.response.AdminAuditLogResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminAuditLogService {

    void record(HttpServletRequest request, HttpServletResponse response, Exception exception);

    Page<AdminAuditLogResponse> getAuditLogs(AuditLogSearchRequest request, Pageable pageable);
}
