package com.cinema.booking.controller;

import com.cinema.booking.dto.request.AuditLogSearchRequest;
import com.cinema.booking.dto.response.ApiResponse;
import com.cinema.booking.dto.response.AuthAuditLogResponse;
import com.cinema.booking.security.service.AuthAuditService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth-audit-logs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthAuditLogController {

    AuthAuditService authAuditService;

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_VIEW')")
    public ApiResponse<Page<AuthAuditLogResponse>> getAuthAuditLogs(
            @ModelAttribute AuditLogSearchRequest request,
            Pageable pageable) {
        return ApiResponse.<Page<AuthAuditLogResponse>>builder()
                .result(authAuditService.search(request, pageable))
                .build();
    }
}
