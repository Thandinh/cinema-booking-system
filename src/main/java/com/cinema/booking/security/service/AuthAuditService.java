package com.cinema.booking.security.service;

import com.cinema.booking.dto.request.AuditLogSearchRequest;
import com.cinema.booking.dto.response.AuthAuditLogResponse;
import com.cinema.booking.entity.AuthAuditLog;
import com.cinema.booking.entity.User;
import com.cinema.booking.repository.AuthAuditLogRepository;
import com.cinema.booking.util.DateRange;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthAuditService {

    AuthAuditLogRepository authAuditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            String eventType,
            User user,
            String username,
            boolean success,
            String failureReason,
            HttpServletRequest request) {
        try {
            AuthAuditLog logEntry = AuthAuditLog.builder()
                    .userId(user == null ? null : user.getId())
                    .username(truncate(resolveUsername(user, username), 255))
                    .eventType(eventType)
                    .success(success)
                    .failureReason(truncate(failureReason, 1000))
                    .ipAddress(truncate(extractClientIp(request), 80))
                    .userAgent(truncate(extractUserAgent(request), 500))
                    .build();

            authAuditLogRepository.save(logEntry);
        } catch (Exception exception) {
            log.warn("Could not persist auth audit log for event {}", eventType, exception);
        }
    }

    @Transactional(readOnly = true)
    public Page<AuthAuditLogResponse> search(AuditLogSearchRequest request, Pageable pageable) {
        AuditLogSearchRequest safeRequest = request == null ? new AuditLogSearchRequest() : request;
        DateRange dateRange = DateRange.of(safeRequest.getFromDate(), safeRequest.getToDate());
        String normalizedEventType = safeRequest.getEventType() == null || safeRequest.getEventType().isBlank()
                ? null
                : safeRequest.getEventType().trim().toUpperCase(Locale.ROOT);
        String keywordPattern = safeRequest.getKeyword() == null || safeRequest.getKeyword().isBlank()
                ? null
                : "%" + safeRequest.getKeyword().trim().toLowerCase(Locale.ROOT) + "%";

        return authAuditLogRepository
                .search(
                         normalizedEventType,
                         safeRequest.getSuccess(),
                         keywordPattern,
                        dateRange.fromSearchBound(),
                        dateRange.toSearchBound(),
                         pageable)
                 .map(this::toResponse);
     }

    private AuthAuditLogResponse toResponse(AuthAuditLog logEntry) {
        return AuthAuditLogResponse.builder()
                .id(logEntry.getId())
                .userId(logEntry.getUserId())
                .username(logEntry.getUsername())
                .eventType(logEntry.getEventType())
                .success(logEntry.getSuccess())
                .failureReason(logEntry.getFailureReason())
                .ipAddress(logEntry.getIpAddress())
                .userAgent(logEntry.getUserAgent())
                .createdAt(logEntry.getCreatedAt())
                .build();
    }

    private String resolveUsername(User user, String username) {
        if (user != null && StringUtils.hasText(user.getUsername())) {
            return user.getUsername();
        }
        return StringUtils.hasText(username) ? username : "anonymous";
    }

    private String extractUserAgent(HttpServletRequest request) {
        if (request == null) return null;
        return request.getHeader("User-Agent");
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
