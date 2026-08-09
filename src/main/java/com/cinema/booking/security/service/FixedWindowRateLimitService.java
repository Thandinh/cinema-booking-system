package com.cinema.booking.security.service;

import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.exception.RateLimitExceededException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FixedWindowRateLimitService {

    private static final int CLEANUP_THRESHOLD = 1_000;

    private final ConcurrentHashMap<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    public void check(String key, int maxAttempts, Duration window, ErrorCode errorCode) {
        if (maxAttempts < 1 || window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Rate limit configuration must be positive");
        }

        Instant now = Instant.now();
        AttemptWindow attemptWindow = attempts.compute(key, (ignored, current) -> {
            if (current == null || current.isExpired(now)) {
                return new AttemptWindow(now.plus(window), 1);
            }
            current.increment();
            return current;
        });

        cleanupExpiredEntries(now);

        if (attemptWindow.attempts() > maxAttempts) {
            throw new RateLimitExceededException(errorCode, attemptWindow.retryAfterSeconds(now));
        }
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    private void cleanupExpiredEntries(Instant now) {
        if (attempts.size() < CLEANUP_THRESHOLD) {
            return;
        }

        Iterator<Map.Entry<String, AttemptWindow>> iterator = attempts.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().isExpired(now)) {
                iterator.remove();
            }
        }
    }

    private static final class AttemptWindow {
        private final Instant expiresAt;
        private int attempts;

        private AttemptWindow(Instant expiresAt, int attempts) {
            this.expiresAt = expiresAt;
            this.attempts = attempts;
        }

        private boolean isExpired(Instant now) {
            return !expiresAt.isAfter(now);
        }

        private void increment() {
            attempts++;
        }

        private int attempts() {
            return attempts;
        }

        private long retryAfterSeconds(Instant now) {
            return Math.max(1, Duration.between(now, expiresAt).toSeconds());
        }
    }
}
