package com.cinema.booking.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationResponse {
    /**
     * Backward-compatible alias for accessToken.
     * New clients should use accessToken explicitly.
     */
    String token;
    String accessToken;
    String refreshToken;
    String tokenType;
    long expiresIn;
    long refreshExpiresIn;
    boolean authenticated;
}
