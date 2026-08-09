package com.cinema.booking.security.service;

import com.cinema.booking.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthRateLimitService {

    private final FixedWindowRateLimitService rateLimitService;

    public void check(String key, int maxAttempts, Duration window) {
        rateLimitService.check(key, maxAttempts, window, ErrorCode.AUTH_RATE_LIMITED);
    }

    public void reset(String key) {
        rateLimitService.reset(key);
    }
}
