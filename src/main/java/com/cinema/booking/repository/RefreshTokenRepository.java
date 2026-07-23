package com.cinema.booking.repository;

import com.cinema.booking.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            update RefreshToken rt
            set rt.revokedAt = :revokedAt,
                rt.revokedReason = :reason
            where rt.user.id = :userId
              and rt.revokedAt is null
            """)
    int revokeAllActiveByUserId(
            @Param("userId") UUID userId,
            @Param("revokedAt") LocalDateTime revokedAt,
            @Param("reason") String reason);

    void deleteAllByExpiresAtBefore(LocalDateTime expiresAt);
}
