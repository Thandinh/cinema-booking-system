package com.cinema.booking.service.impl;

import com.cinema.booking.dto.request.AuditLogSearchRequest;
import com.cinema.booking.dto.response.AdminAuditLogResponse;
import com.cinema.booking.entity.AdminAuditLog;
import com.cinema.booking.repository.AdminAuditLogRepository;
import com.cinema.booking.service.AdminAuditLogService;
import com.cinema.booking.util.DateRange;
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
    public Page<AdminAuditLogResponse> getAuditLogs(AuditLogSearchRequest request, Pageable pageable) {
        AuditLogSearchRequest safeRequest = request == null ? new AuditLogSearchRequest() : request;
        DateRange dateRange = DateRange.of(safeRequest.getFromDate(), safeRequest.getToDate());
        String keywordPattern = safeRequest.getKeyword() == null || safeRequest.getKeyword().isBlank()
                ? null
                : "%" + safeRequest.getKeyword().trim().toLowerCase(Locale.ROOT) + "%";
        String normalizedAction = normalizeFilter(safeRequest.getAction());
        String normalizedResource = normalizeFilter(safeRequest.getResource());

        return adminAuditLogRepository
                .search(
                        normalizedAction,
                         normalizedResource,
                         safeRequest.getSuccess(),
                         keywordPattern,
                        dateRange.fromSearchBound(),
                        dateRange.toSearchBound(),
                         pageable)
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

        if (isPublicAccountFlow(parts)) {
            return null;
        }

        String resource = normalizeResource(parts);
        if (resource == null) {
            return null;
        }

        String resourceId = resolveResourceId(parts);
        String action = resolveAction(method, parts);

        return new AuditTarget(action, resource, resourceId);
    }

    private boolean isPublicAccountFlow(String[] parts) {
        return parts.length >= 4
                && "users".equals(parts[2])
                && switch (parts[3]) {
                    case "register", "verify-email", "resend-verification", "forgot-password", "reset-password" -> true;
                    default -> false;
                };
    }

    private String normalizeResource(String[] parts) {
        if ("bookings".equals(parts[2]) && parts.length >= 4 && "tickets".equals(parts[3])) {
            return "TICKET";
        }
        return switch (parts[2]) {
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

    private String resolveResourceId(String[] parts) {
        if ("bookings".equals(parts[2]) && parts.length >= 4 && !"tickets".equals(parts[3])) {
            return truncate(parts[3], 100);
        }
        if ("bookings".equals(parts[2]) && parts.length >= 5 && "tickets".equals(parts[3])) {
            return truncate(parts[4], 100);
        }
        if ("tickets".equals(parts[2]) && parts.length >= 4 && !"check-in".equals(parts[3])) {
            return truncate(parts[3], 100);
        }
        if (parts.length >= 4 && isLikelyResourceId(parts[3])) {
            return truncate(parts[3], 100);
        }
        return null;
    }

    private String resolveAction(String method, String[] parts) {
        String httpMethod = method.toUpperCase(Locale.ROOT);

        if ("tickets".equals(parts[2]) && containsPathPart(parts, "check-in")) {
            return "CHECK_IN_TICKET";
        }
        if ("bookings".equals(parts[2]) && containsPathPart(parts, "tickets") && containsPathPart(parts, "check-in")) {
            return "CHECK_IN_TICKET";
        }
        if ("bookings".equals(parts[2]) && containsPathPart(parts, "hold")) {
            return "HOLD_SEATS";
        }
        if ("bookings".equals(parts[2]) && containsPathPart(parts, "cancel")) {
            return "CANCEL_BOOKING";
        }
        if ("bookings".equals(parts[2]) && containsPathPart(parts, "promotion")) {
            return "DELETE".equals(httpMethod) ? "REMOVE_PROMOTION" : "APPLY_PROMOTION";
        }
        if ("payments".equals(parts[2]) && containsPathPart(parts, "initiate")) {
            return "INITIATE_PAYMENT";
        }
        if ("payments".equals(parts[2]) && containsPathPart(parts, "momo-ipn")) {
            return "PAYMENT_IPN";
        }
        if ("users".equals(parts[2]) && containsPathPart(parts, "block")) {
            return "BLOCK_USER";
        }
        if ("users".equals(parts[2]) && containsPathPart(parts, "unblock")) {
            return "UNBLOCK_USER";
        }
        if ("users".equals(parts[2]) && containsPathPart(parts, "password")) {
            return "CHANGE_PASSWORD";
        }
        if ("users".equals(parts[2]) && "me".equals(pathPart(parts, 3))) {
            return "UPDATE_PROFILE";
        }
        if ("users".equals(parts[2]) && ("PUT".equals(httpMethod) || "PATCH".equals(httpMethod))) {
            return "UPDATE_USER_AND_SCOPE";
        }
        if ("seats".equals(parts[2]) && containsPathPart(parts, "bulk-generate")) {
            return "GENERATE_SEATS";
        }

        return switch (httpMethod) {
            case "POST" -> "CREATE";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> httpMethod;
        };
    }

    private boolean containsPathPart(String[] parts, String expected) {
        return Arrays.asList(parts).contains(expected);
    }

    private String pathPart(String[] parts, int index) {
        return index < parts.length ? parts[index] : null;
    }

    private boolean isLikelyResourceId(String value) {
        return value != null
                && !"me".equals(value)
                && !"my".equals(value)
                && !"hold".equals(value)
                && !"bulk-generate".equals(value)
                && !"check-in".equals(value)
                && !"initiate".equals(value)
                && !"events".equals(value)
                && !"reconciliation".equals(value);
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
