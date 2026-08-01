package com.cinema.booking.security.jwt;

import com.cinema.booking.configuration.JwtProperties;
import com.cinema.booking.dto.request.IntrospectRequest;
import com.cinema.booking.security.service.AuthenticationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomJwtDecoder implements JwtDecoder {

//    @Value("${jwt.signer-key}")
//    String signerKey;
    private final JwtProperties jwtProperties;

    final AuthenticationService authenticationService;
    volatile NimbusJwtDecoder nimbusJwtDecoder;

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            ensureTokenIsActive(token);
            return getNimbusJwtDecoder().decode(token);
        } catch (BadJwtException exception) {
            throw exception;
        } catch (JwtException exception) {
            throw new BadJwtException("Invalid token", exception);
        }
    }

    private void ensureTokenIsActive(String token) {
        // Database-backed checks: token type, expiry, logout blacklist, and user state.
        var response = authenticationService.introspect(IntrospectRequest.builder().token(token).build());
        if (!response.isValid()) {
            throw new BadJwtException("Invalid token");
        }
    }

    private synchronized NimbusJwtDecoder getNimbusJwtDecoder() {
        if (Objects.isNull(nimbusJwtDecoder)) {
            SecretKeySpec secretKeySpec = new SecretKeySpec(jwtProperties.getSignerKey().getBytes(), "HS512");
            nimbusJwtDecoder = NimbusJwtDecoder.withSecretKey(secretKeySpec)
                    .macAlgorithm(MacAlgorithm.HS512)
                    .build();
        }
        return nimbusJwtDecoder;
    }
}
