package com.cinema.booking.security.service;

import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.exception.AppException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthRateLimitService {

    ConcurrentHashMap<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    public void check(String key, int maxAttempts, Duration window) {
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
            throw new AppException(ErrorCode.AUTH_RATE_LIMITED);
        }
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    private void cleanupExpiredEntries(Instant now) {
        if (attempts.size() < 1_000) {
            return;
        }

        Iterator<Map.Entry<String, AttemptWindow>> iterator = attempts.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, AttemptWindow> entry = iterator.next();
            if (entry.getValue().isExpired(now)) {
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
    }
}
