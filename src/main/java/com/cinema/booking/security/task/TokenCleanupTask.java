package com.cinema.booking.security.task;

import com.cinema.booking.repository.InvalidatedTokenRepository;
import com.cinema.booking.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;

@Component
@ConditionalOnProperty(prefix = "booking.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
@RequiredArgsConstructor
public class TokenCleanupTask {
    private final InvalidatedTokenRepository invalidatedTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting expired token cleanup");
        try {
            invalidatedTokenRepository.deleteAllByExpiryTimeBefore(new Date());
            refreshTokenRepository.deleteAllByExpiresAtBefore(LocalDateTime.now());
            log.info("Expired token cleanup completed");
        } catch (Exception e) {
            log.error("Failed to clean expired tokens: {}", e.getMessage());
        }
    }
}
