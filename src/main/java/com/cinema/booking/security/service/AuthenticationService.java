package com.cinema.booking.security.service;

import com.cinema.booking.configuration.GoogleOAuthProperties;
import com.cinema.booking.configuration.JwtProperties;
import com.cinema.booking.dto.request.AuthenticationRequest;
import com.cinema.booking.dto.request.GoogleLoginRequest;
import com.cinema.booking.dto.request.IntrospectRequest;
import com.cinema.booking.dto.request.LogoutRequest;
import com.cinema.booking.dto.request.RefreshRequest;
import com.cinema.booking.dto.response.AuthenticationResponse;
import com.cinema.booking.dto.response.IntrospectResponse;
import com.cinema.booking.entity.InvalidatedToken;
import com.cinema.booking.entity.RefreshToken;
import com.cinema.booking.entity.Role;
import com.cinema.booking.entity.User;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.repository.InvalidatedTokenRepository;
import com.cinema.booking.repository.RefreshTokenRepository;
import com.cinema.booking.repository.RoleRepository;
import com.cinema.booking.repository.UserRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    static final String ISSUER = "cinema-booking";
    static final String TOKEN_USE_CLAIM = "token_use";
    static final String ACCESS_TOKEN_USE = "access";
    static final String REFRESH_TOKEN_USE = "refresh";
    static final String GOOGLE_ISSUER = "https://accounts.google.com";
    static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    UserRepository userRepository;
    RoleRepository roleRepository;
    InvalidatedTokenRepository invalidatedTokenRepository;
    RefreshTokenRepository refreshTokenRepository;
    JwtProperties jwtProperties;
    GoogleOAuthProperties googleOAuthProperties;

    public IntrospectResponse introspect(IntrospectRequest request) {
        boolean isValid = true;
        try {
            verifyToken(request.getToken(), false);
        } catch (AppException | JOSEException | ParseException e) {
            isValid = false;
        }
        return IntrospectResponse.builder().valid(isValid).build();
    }

    public String generateToken(User user) {
        return generateAccessToken(user);
    }

    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request, HttpServletRequest servletRequest) {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        validateUserCanAuthenticate(user);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return issueTokenPair(user, servletRequest);
    }

    @Transactional
    public AuthenticationResponse authenticateWithGoogle(GoogleLoginRequest request, HttpServletRequest servletRequest) {
        Jwt googleJwt = decodeGoogleIdToken(request.getIdToken());

        Boolean emailVerified = googleJwt.getClaim("email_verified");
        String email = googleJwt.getClaimAsString("email");
        if (!Boolean.TRUE.equals(emailVerified) || !StringUtils.hasText(email)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .map(existingUser -> {
                    if (Boolean.FALSE.equals(existingUser.getEmailVerified())) {
                        existingUser.setEmailVerified(true);
                        existingUser.setEmailVerificationTokenHash(null);
                        existingUser.setEmailVerificationExpiresAt(null);
                    }
                    updateMissingGoogleAvatar(existingUser, googleJwt);
                    validateUserCanAuthenticate(existingUser);
                    return existingUser;
                })
                .orElseGet(() -> createGoogleUser(googleJwt, email));

        return issueTokenPair(user, servletRequest);
    }

    @Transactional
    public AuthenticationResponse refreshToken(RefreshRequest request, HttpServletRequest servletRequest)
            throws ParseException, JOSEException {
        String refreshToken = request.getToken();
        verifyToken(refreshToken, true);
        String tokenHash = hashToken(refreshToken);
        RefreshToken currentRefreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        User user = currentRefreshToken.getUser();
        validateUserCanAuthenticate(user);

        String newRefreshToken = generateRefreshToken(user, servletRequest);
        String newRefreshTokenId = SignedJWT.parse(newRefreshToken).getJWTClaimsSet().getJWTID();

        currentRefreshToken.setRevokedAt(LocalDateTime.now());
        currentRefreshToken.setRevokedReason("ROTATED");
        currentRefreshToken.setReplacedByTokenId(newRefreshTokenId);
        refreshTokenRepository.save(currentRefreshToken);

        String accessToken = generateAccessToken(user);
        return buildAuthenticationResponse(accessToken, newRefreshToken);
    }

    @Transactional
    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        invalidateAccessToken(request.getToken());
        revokeRefreshToken(request.getRefreshToken(), "LOGOUT");
    }

    public SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        if (!StringUtils.hasText(token)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        SignedJWT signedJWT = verifySignedToken(token);
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        String tokenUse = claims.getStringClaim(TOKEN_USE_CLAIM);
        String expectedTokenUse = isRefresh ? REFRESH_TOKEN_USE : ACCESS_TOKEN_USE;
        if (!expectedTokenUse.equals(tokenUse)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        Date expiresAt = claims.getExpirationTime();
        if (expiresAt == null || !expiresAt.after(new Date())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (isRefresh) {
            validateRefreshTokenRecord(token, claims);
        } else if (invalidatedTokenRepository.existsById(claims.getJWTID())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        User user = userRepository.findByUsername(claims.getSubject())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        validateUserCanAuthenticate(user);

        return signedJWT;
    }

    private AuthenticationResponse issueTokenPair(User user, HttpServletRequest servletRequest) {
        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user, servletRequest);
        return buildAuthenticationResponse(accessToken, refreshToken);
    }

    private AuthenticationResponse buildAuthenticationResponse(String accessToken, String refreshToken) {
        return AuthenticationResponse.builder()
                .token(accessToken)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenValidDuration())
                .refreshExpiresIn(jwtProperties.getRefreshTokenValidDuration())
                .authenticated(true)
                .build();
    }

    private String generateAccessToken(User user) {
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer(ISSUER)
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now()
                        .plus(jwtProperties.getAccessTokenValidDuration(), ChronoUnit.SECONDS)
                        .toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim(TOKEN_USE_CLAIM, ACCESS_TOKEN_USE)
                .claim("scope", buildScope(user))
                .claim("userId", user.getId().toString())
                .build();

        return sign(jwtClaimsSet);
    }

    private String generateRefreshToken(User user, HttpServletRequest servletRequest) {
        String tokenId = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(jwtProperties.getRefreshTokenValidDuration(), ChronoUnit.SECONDS);
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer(ISSUER)
                .issueTime(new Date())
                .expirationTime(Date.from(expiresAt))
                .jwtID(tokenId)
                .claim(TOKEN_USE_CLAIM, REFRESH_TOKEN_USE)
                .claim("userId", user.getId().toString())
                .build();

        String refreshToken = sign(jwtClaimsSet);
        refreshTokenRepository.save(RefreshToken.builder()
                .tokenHash(hashToken(refreshToken))
                .tokenId(tokenId)
                .user(user)
                .expiresAt(LocalDateTime.ofInstant(expiresAt, SYSTEM_ZONE))
                .userAgent(extractUserAgent(servletRequest))
                .ipAddress(extractClientIp(servletRequest))
                .build());
        return refreshToken;
    }

    private String sign(JWTClaimsSet jwtClaimsSet) {
        JWSObject jwsObject = new JWSObject(new JWSHeader(JWSAlgorithm.HS512), new Payload(jwtClaimsSet.toJSONObject()));
        try {
            jwsObject.sign(new MACSigner(jwtProperties.getSignerKey().getBytes(StandardCharsets.UTF_8)));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Error signing token", e);
        }
    }

    private SignedJWT verifySignedToken(String token) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(jwtProperties.getSignerKey().getBytes(StandardCharsets.UTF_8));
        SignedJWT signedJWT = SignedJWT.parse(token);
        if (!signedJWT.verify(verifier)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return signedJWT;
    }

    private void validateRefreshTokenRecord(String token, JWTClaimsSet claims) {
        LocalDateTime now = LocalDateTime.now();
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hashToken(token))
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        if (!refreshToken.getTokenId().equals(claims.getJWTID())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (!refreshToken.isActive(now)) {
            if (refreshToken.getRevokedAt() != null) {
                refreshTokenRepository.revokeAllActiveByUserId(
                        refreshToken.getUser().getId(),
                        now,
                        "REUSE_DETECTED");
                log.warn("Refresh token reuse detected for user {}", refreshToken.getUser().getUsername());
            }
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    private void invalidateAccessToken(String token) throws ParseException, JOSEException {
        try {
            SignedJWT signedToken = verifyToken(token, false);
            JWTClaimsSet claims = signedToken.getJWTClaimsSet();
            invalidatedTokenRepository.save(InvalidatedToken.builder()
                    .id(claims.getJWTID())
                    .expiryTime(claims.getExpirationTime())
                    .build());
        } catch (AppException e) {
            log.info("Access token already expired or invalid");
        }
    }

    private void revokeRefreshToken(String token, String reason) {
        if (!StringUtils.hasText(token)) {
            return;
        }

        refreshTokenRepository.findByTokenHash(hashToken(token)).ifPresent(refreshToken -> {
            if (refreshToken.getRevokedAt() == null) {
                refreshToken.setRevokedAt(LocalDateTime.now());
                refreshToken.setRevokedReason(reason);
                refreshTokenRepository.save(refreshToken);
            }
        });
    }

    private void validateUserCanAuthenticate(User user) {
        if (Boolean.FALSE.equals(user.getIsActive()) || Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new AppException(ErrorCode.USER_NOT_ACTIVE);
        }
        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            throw new AppException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    private Jwt decodeGoogleIdToken(String idToken) {
        String clientId = googleOAuthProperties.getClientId();
        if (!StringUtils.hasText(clientId)) {
            log.warn("Google login attempted but oauth.google.client-id is not configured");
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI).build();
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(GOOGLE_ISSUER);
        OAuth2TokenValidator<Jwt> audienceValidator = token ->
                token.getAudience().contains(clientId)
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_token",
                        "Google ID token audience does not match this application",
                        null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));

        try {
            return decoder.decode(idToken);
        } catch (JwtException exception) {
            log.warn("Invalid Google ID token: {}", exception.getMessage());
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    private User createGoogleUser(Jwt googleJwt, String email) {
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        String givenName = googleJwt.getClaimAsString("given_name");
        String familyName = googleJwt.getClaimAsString("family_name");
        String fullName = googleJwt.getClaimAsString("name");
        String picture = googleJwt.getClaimAsString("picture");

        User user = User.builder()
                .username(generateUniqueGoogleUsername(email))
                .password(new BCryptPasswordEncoder(10).encode(UUID.randomUUID().toString()))
                .firstName(resolveFirstName(givenName, fullName))
                .lastName(resolveLastName(familyName, givenName, fullName))
                .email(email.toLowerCase(Locale.ROOT))
                .avatarUrl(StringUtils.hasText(picture) ? picture : null)
                .emailVerified(true)
                .emailVerificationTokenHash(null)
                .emailVerificationExpiresAt(null)
                .isActive(true)
                .isDeleted(false)
                .roles(new HashSet<>(Set.of(userRole)))
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new Google user: {}", savedUser.getUsername());
        return savedUser;
    }

    private void updateMissingGoogleAvatar(User user, Jwt googleJwt) {
        String picture = googleJwt.getClaimAsString("picture");
        if (!StringUtils.hasText(user.getAvatarUrl()) && StringUtils.hasText(picture)) {
            user.setAvatarUrl(picture);
        }
    }

    private String generateUniqueGoogleUsername(String email) {
        String localPart = email.substring(0, email.indexOf('@'))
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        String base = localPart.length() >= 4 ? localPart : "user_" + localPart;
        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }

    private String resolveFirstName(String givenName, String fullName) {
        if (StringUtils.hasText(givenName)) return givenName;
        if (!StringUtils.hasText(fullName)) return "Google";
        String[] parts = fullName.trim().split("\\s+");
        return parts[parts.length - 1];
    }

    private String resolveLastName(String familyName, String givenName, String fullName) {
        if (StringUtils.hasText(familyName)) return familyName;
        if (StringUtils.hasText(fullName)) {
            String trimmedName = fullName.trim();
            if (StringUtils.hasText(givenName) && trimmedName.endsWith(givenName.trim())) {
                String inferredLastName = trimmedName.substring(0, trimmedName.length() - givenName.trim().length()).trim();
                if (StringUtils.hasText(inferredLastName)) return inferredLastName;
            }

            String[] parts = trimmedName.split("\\s+");
            if (parts.length > 1) {
                return String.join(" ", java.util.Arrays.copyOf(parts, parts.length - 1));
            }
        }
        return "";
    }

    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (!CollectionUtils.isEmpty(user.getRoles())) {
            user.getRoles().forEach(role -> {
                stringJoiner.add("ROLE_" + role.getName());
                if (!CollectionUtils.isEmpty(role.getPermissions())) {
                    role.getPermissions().forEach(p -> stringJoiner.add(p.getName()));
                }
            });
        }
        return stringJoiner.toString();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private String extractUserAgent(HttpServletRequest request) {
        if (request == null) return null;
        String userAgent = request.getHeader("User-Agent");
        if (!StringUtils.hasText(userAgent)) return null;
        return userAgent.length() <= 500 ? userAgent : userAgent.substring(0, 500);
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String ip = StringUtils.hasText(forwardedFor)
                ? forwardedFor.split(",")[0].trim()
                : request.getRemoteAddr();
        if (!StringUtils.hasText(ip)) return null;
        return ip.length() <= 80 ? ip : ip.substring(0, 80);
    }
}
