package com.cinema.booking.security.jwt;

import com.cinema.booking.configuration.JwtProperties;
import com.cinema.booking.dto.request.IntrospectRequest;
import com.cinema.booking.security.service.AuthenticationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
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
    NimbusJwtDecoder nimbusJwtDecoder = null;

    @Override
    public Jwt decode(String token) throws JwtException {

        // 1. Kiểm tra Introspect trước (Database check)
        var response = authenticationService.introspect(IntrospectRequest.builder().token(token).build());
        if (!response.isValid()) throw new JwtException("Invalid token");

        // 2. Nếu OK, tiến hành decode
        if (Objects.isNull(nimbusJwtDecoder)) {
            SecretKeySpec secretKeySpec = new SecretKeySpec(jwtProperties.getSignerKey().getBytes(), "HS512");
            nimbusJwtDecoder = NimbusJwtDecoder.withSecretKey(secretKeySpec)
                    .macAlgorithm(MacAlgorithm.HS512)
                    .build();
        }
        return nimbusJwtDecoder.decode(token);
    }
}