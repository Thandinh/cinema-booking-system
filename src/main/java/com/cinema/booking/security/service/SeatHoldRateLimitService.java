package com.cinema.booking.security.service;

import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Service
public class SeatHoldRateLimitService {

    private final FixedWindowRateLimitService rateLimitService;
    private final boolean enabled;
    private final int userMaxRequests;
    private final int ipMaxRequests;
    private final Duration window;

    public SeatHoldRateLimitService(
            FixedWindowRateLimitService rateLimitService,
            @Value("${booking.hold-rate-limit.enabled:true}") boolean enabled,
            @Value("${booking.hold-rate-limit.user-max-requests:12}") int userMaxRequests,
            @Value("${booking.hold-rate-limit.ip-max-requests:60}") int ipMaxRequests,
            @Value("${booking.hold-rate-limit.window-seconds:60}") long windowSeconds) {
        this.rateLimitService = rateLimitService;
        this.enabled = enabled;
        this.userMaxRequests = userMaxRequests;
        this.ipMaxRequests = ipMaxRequests;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    public void check(HttpServletRequest request) {
        if (!enabled) {
            return;
        }

        String userId = SecurityUtils.getCurrentUserId().toString();
        String clientIp = extractClientIp(request);
        rateLimitService.check(
                "seat-hold:user:" + userId,
                userMaxRequests,
                window,
                ErrorCode.SEAT_HOLD_RATE_LIMITED);
        rateLimitService.check(
                "seat-hold:ip:" + clientIp,
                ipMaxRequests,
                window,
                ErrorCode.SEAT_HOLD_RATE_LIMITED);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }
}
