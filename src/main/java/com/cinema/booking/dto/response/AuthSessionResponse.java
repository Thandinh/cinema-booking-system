package com.cinema.booking.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthSessionResponse {
    UUID id;
    Boolean current;
    String ipAddress;
    String userAgent;
    LocalDateTime createdAt;
    LocalDateTime expiresAt;
    LocalDateTime revokedAt;
    String revokedReason;
}
