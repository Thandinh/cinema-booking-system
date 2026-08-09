package com.cinema.booking.security.service;

import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FixedWindowRateLimitServiceTest {

    private final FixedWindowRateLimitService rateLimitService = new FixedWindowRateLimitService();

    @Test
    void check_shouldRejectRequestsAboveConfiguredLimit() {
        String key = "seat-hold:user:test";

        rateLimitService.check(key, 2, Duration.ofMinutes(1), ErrorCode.SEAT_HOLD_RATE_LIMITED);
        rateLimitService.check(key, 2, Duration.ofMinutes(1), ErrorCode.SEAT_HOLD_RATE_LIMITED);

        assertThatThrownBy(() -> rateLimitService.check(
                key,
                2,
                Duration.ofMinutes(1),
                ErrorCode.SEAT_HOLD_RATE_LIMITED))
                .isInstanceOfSatisfying(RateLimitExceededException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SEAT_HOLD_RATE_LIMITED);
                    assertThat(exception.getRetryAfterSeconds()).isPositive();
                });
    }

    @Test
    void reset_shouldClearAttemptWindow() {
        String key = "login:test";
        rateLimitService.check(key, 1, Duration.ofMinutes(1), ErrorCode.AUTH_RATE_LIMITED);

        rateLimitService.reset(key);

        rateLimitService.check(key, 1, Duration.ofMinutes(1), ErrorCode.AUTH_RATE_LIMITED);
    }
}
