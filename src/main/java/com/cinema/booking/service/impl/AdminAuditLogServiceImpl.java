package com.cinema.booking.service.impl;

import com.cinema.booking.dto.response.AdminAuditLogResponse;
import com.cinema.booking.entity.AdminAuditLog;
import com.cinema.booking.repository.AdminAuditLogRepository;
import com.cinema.booking.service.AdminAuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AdminAuditLogServiceImpl implements AdminAuditLogService {

    private static final int MAX_TEXT_LENGTH = 500;

    AdminAuditLogRepository adminAuditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(HttpServletRequest request, HttpServletResponse response, Exception exception) {
        try {
            AuditTarget target = resolveTarget(request);
            if (target == null) {
                return;
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UUID actorId = resolveActorId(authentication);
            String actorUsername = authentication == null ? "anonymous" : authentication.getName();
            int statusCode = response.getStatus();

            AdminAuditLog logEntry = AdminAuditLog.builder()
                    .actorId(actorId)
                    .actorUsername(truncate(actorUsername, 255))
                    .httpMethod(request.getMethod())
                    .action(target.action())
                    .resource(target.resource())
                    .resourceId(target.resourceId())
                    .requestPath(truncate(request.getRequestURI(), MAX_TEXT_LENGTH))
                    .queryString(truncate(request.getQueryString(), MAX_TEXT_LENGTH))
                    .ipAddress(truncate(resolveClientIp(request), 80))
                    .userAgent(truncate(request.getHeader("User-Agent"), MAX_TEXT_LENGTH))
                    .statusCode(statusCode)
                    .success(exception == null && statusCode < 400)
                    .errorMessage(exception == null ? null : truncate(exception.getMessage(), 1000))
                    .build();

            adminAuditLogRepository.save(logEntry);
        } catch (Exception auditException) {
            log.warn("Could not persist admin audit log for {} {}", request.getMethod(), request.getRequestURI(), auditException);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminAuditLogResponse> getAuditLogs(
            Pageable pageable,
            String action,
            String resource,
            Boolean success,
            String keyword) {
        String keywordPattern = keyword == null || keyword.isBlank()
                ? null
                : "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        String normalizedAction = normalizeFilter(action);
        String normalizedResource = normalizeFilter(resource);

        return adminAuditLogRepository
                .search(normalizedAction, normalizedResource, success, keywordPattern, pageable)
                .map(this::toResponse);
    }

    private AdminAuditLogResponse toResponse(AdminAuditLog logEntry) {
        return AdminAuditLogResponse.builder()
                .id(logEntry.getId())
                .actorId(logEntry.getActorId())
                .actorUsername(logEntry.getActorUsername())
                .httpMethod(logEntry.getHttpMethod())
                .action(logEntry.getAction())
                .resource(logEntry.getResource())
                .resourceId(logEntry.getResourceId())
                .requestPath(logEntry.getRequestPath())
                .queryString(logEntry.getQueryString())
                .ipAddress(logEntry.getIpAddress())
                .userAgent(logEntry.getUserAgent())
                .statusCode(logEntry.getStatusCode())
                .success(logEntry.getSuccess())
                .errorMessage(logEntry.getErrorMessage())
                .createdAt(logEntry.getCreatedAt())
                .build();
    }

    private AuditTarget resolveTarget(HttpServletRequest request) {
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            return null;
        }

        String[] parts = Arrays.stream(request.getRequestURI().split("/"))
                .filter(part -> !part.isBlank())
                .toArray(String[]::new);
        if (parts.length < 3 || !"api".equals(parts[0]) || !"v1".equals(parts[1])) {
            return null;
        }

        String resource = normalizeResource(parts[2]);
        if (resource == null) {
            return null;
        }

        String resourceId = parts.length >= 4 ? truncate(parts[3], 100) : null;
        String action = switch (method.toUpperCase(Locale.ROOT)) {
            case "POST" -> "CREATE";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> method.toUpperCase(Locale.ROOT);
        };

        if ("tickets".equals(parts[2]) && request.getRequestURI().contains("/check-in")) {
            action = "CHECK_IN";
        }
        if ("bookings".equals(parts[2]) && request.getRequestURI().contains("/cancel")) {
            action = "CANCEL";
        }

        return new AuditTarget(action, resource, resourceId);
    }

    private String normalizeResource(String pathPart) {
        return switch (pathPart) {
            case "movies" -> "MOVIE";
            case "cinemas" -> "CINEMA";
            case "rooms" -> "ROOM";
            case "seats" -> "SEAT";
            case "showtimes" -> "SHOWTIME";
            case "bookings" -> "BOOKING";
            case "payments" -> "PAYMENT";
            case "promotions" -> "PROMOTION";
            case "users" -> "USER";
            case "tickets" -> "TICKET";
            default -> null;
        };
    }

    private UUID resolveActorId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }
        String userId = jwt.getClaimAsString("userId");
        if (userId == null || userId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String normalizeFilter(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record AuditTarget(String action, String resource, String resourceId) {
    }
}
