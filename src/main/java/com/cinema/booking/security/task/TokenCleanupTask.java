package com.cinema.booking.security.task;

import com.cinema.booking.repository.InvalidatedTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
@RequiredArgsConstructor
public class TokenCleanupTask {
    private final InvalidatedTokenRepository invalidatedTokenRepository;

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("--- BẮT ĐẦU DỌN DẸP TOKEN HẾT HẠN ---");
        try {
            invalidatedTokenRepository.deleteAllByExpiryTimeBefore(new Date());
            log.info("--- DỌN DẸP HOÀN TẤT ---");
        } catch (Exception e) {
            log.error("Lỗi khi dọn dẹp Token: {}", e.getMessage());
        }
    }

}
