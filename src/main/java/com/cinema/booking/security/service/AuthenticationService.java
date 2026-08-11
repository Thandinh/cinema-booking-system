package com.cinema.booking.security.service;

import com.cinema.booking.configuration.GoogleOAuthProperties;
import com.cinema.booking.configuration.JwtProperties;
import com.cinema.booking.dto.request.AuthenticationRequest;
import com.cinema.booking.dto.request.GoogleLoginRequest;
import com.cinema.booking.dto.request.IntrospectRequest;
import com.cinema.booking.dto.request.LogoutRequest;
import com.cinema.booking.dto.request.RefreshRequest;
import com.cinema.booking.dto.response.AuthSessionResponse;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
    static final String AUTH_VERSION_CLAIM = "auth_version";
    static final String ACCESS_TOKEN_USE = "access";
    static final String REFRESH_TOKEN_USE = "refresh";
    static final String EVENT_LOGIN_PASSWORD = "LOGIN_PASSWORD";
    static final String EVENT_LOGIN_GOOGLE = "LOGIN_GOOGLE";
    static final String EVENT_REFRESH_TOKEN = "REFRESH_TOKEN";
    static final String EVENT_LOGOUT = "LOGOUT";
    static final String EVENT_REVOKE_SESSION = "REVOKE_SESSION";
    static final String GOOGLE_ISSUER = "https://accounts.google.com";
    static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();
    static final int LOGIN_MAX_ATTEMPTS = 5;
    static final int GOOGLE_LOGIN_MAX_ATTEMPTS = 15;
    static final int REFRESH_MAX_ATTEMPTS = 60;
    static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);
    static final Duration REFRESH_WINDOW = Duration.ofMinutes(1);

    UserRepository userRepository;
    RoleRepository roleRepository;
    InvalidatedTokenRepository invalidatedTokenRepository;
    RefreshTokenRepository refreshTokenRepository;
    JwtProperties jwtProperties;
    GoogleOAuthProperties googleOAuthProperties;
    AuthRateLimitService authRateLimitService;
    AuthAuditService authAuditService;
    PlatformTransactionManager transactionManager;
    PasswordEncoder passwordEncoder;

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
        String username = normalizeUsername(request.getUsername());
        String rateLimitKey = loginRateLimitKey(username, servletRequest);
        User user = null;
        try {
            authRateLimitService.check(rateLimitKey, LOGIN_MAX_ATTEMPTS, LOGIN_WINDOW);

            user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            validateUserCanAuthenticate(user);

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }

            AuthenticationResponse response = issueTokenPair(user, servletRequest);
            authRateLimitService.reset(rateLimitKey);
            authAuditService.record(EVENT_LOGIN_PASSWORD, user, username, true, null, servletRequest);
            return response;
        } catch (RuntimeException exception) {
            authAuditService.record(EVENT_LOGIN_PASSWORD, user, username, false, resolveFailureReason(exception), servletRequest);
            throw exception;
        }
    }

    @Transactional
    public AuthenticationResponse authenticateWithGoogle(GoogleLoginRequest request, HttpServletRequest servletRequest) {
        String rateLimitKey = genericRateLimitKey("google", servletRequest);
        User user = null;
        String email = null;
        try {
            authRateLimitService.check(rateLimitKey, GOOGLE_LOGIN_MAX_ATTEMPTS, LOGIN_WINDOW);
            Jwt googleJwt = decodeGoogleIdToken(request.getIdToken());

            Boolean emailVerified = googleJwt.getClaim("email_verified");
            email = googleJwt.getClaimAsString("email");
            if (!Boolean.TRUE.equals(emailVerified) || !StringUtils.hasText(email)) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }

            String verifiedEmail = email;
            user = userRepository.findByEmailIgnoreCase(verifiedEmail)
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
                    .orElseGet(() -> createGoogleUser(googleJwt, verifiedEmail));

            AuthenticationResponse response = issueTokenPair(user, servletRequest);
            authAuditService.record(EVENT_LOGIN_GOOGLE, user, email, true, null, servletRequest);
            return response;
        } catch (RuntimeException exception) {
            authAuditService.record(EVENT_LOGIN_GOOGLE, user, email, false, resolveFailureReason(exception), servletRequest);
            throw exception;
        }
    }

    @Transactional
    public AuthenticationResponse refreshToken(RefreshRequest request, HttpServletRequest servletRequest)
            throws ParseException, JOSEException {
        String refreshToken = request.getToken();
        User user = null;
        try {
            authRateLimitService.check(genericRateLimitKey("refresh", servletRequest), REFRESH_MAX_ATTEMPTS, REFRESH_WINDOW);
            verifyToken(refreshToken, true);
            String tokenHash = hashToken(refreshToken);
            RefreshToken currentRefreshToken = refreshTokenRepository.findLockedByTokenHash(tokenHash)
                    .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

            validateRefreshTokenRecord(currentRefreshToken, SignedJWT.parse(refreshToken).getJWTClaimsSet());

            user = currentRefreshToken.getUser();
            validateUserCanAuthenticate(user);

            String newRefreshToken = generateRefreshToken(user, servletRequest);
            String newRefreshTokenId = SignedJWT.parse(newRefreshToken).getJWTClaimsSet().getJWTID();

            currentRefreshToken.setRevokedAt(LocalDateTime.now());
            currentRefreshToken.setRevokedReason("ROTATED");
            currentRefreshToken.setReplacedByTokenId(newRefreshTokenId);
            refreshTokenRepository.save(currentRefreshToken);

            String accessToken = generateAccessToken(user);
            authAuditService.record(EVENT_REFRESH_TOKEN, user, user.getUsername(), true, null, servletRequest);
            return buildAuthenticationResponse(accessToken, newRefreshToken);
        } catch (RuntimeException | ParseException | JOSEException exception) {
            authAuditService.record(EVENT_REFRESH_TOKEN, user, null, false, resolveFailureReason(exception), servletRequest);
            throw exception;
        }
    }

    @Transactional
    public void logout(LogoutRequest request, HttpServletRequest servletRequest) throws ParseException, JOSEException {
        User user = resolveUserFromRefreshToken(request.getRefreshToken());
        try {
            invalidateAccessToken(request.getToken());
            revokeRefreshToken(request.getRefreshToken(), "LOGOUT");
            authAuditService.record(EVENT_LOGOUT, user, user == null ? null : user.getUsername(), true, null, servletRequest);
        } catch (RuntimeException | ParseException | JOSEException exception) {
            authAuditService.record(EVENT_LOGOUT, user, user == null ? null : user.getUsername(), false, resolveFailureReason(exception), servletRequest);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<AuthSessionResponse> getSessions(String username, String currentRefreshToken) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        String currentRefreshTokenHash = StringUtils.hasText(currentRefreshToken) ? hashToken(currentRefreshToken) : null;

        return refreshTokenRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(refreshToken -> toSessionResponse(refreshToken, currentRefreshTokenHash))
                .toList();
    }

    @Transactional
    public boolean revokeSession(String username, UUID sessionId, String currentRefreshToken, HttpServletRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        RefreshToken refreshToken = refreshTokenRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        boolean isCurrent = StringUtils.hasText(currentRefreshToken)
                && hashToken(currentRefreshToken).equals(refreshToken.getTokenHash());
        revokeRefreshTokenRecord(refreshToken, "SESSION_REVOKED");
        authAuditService.record(EVENT_REVOKE_SESSION, user, user.getUsername(), true,
                "sessionId=" + sessionId + ", current=" + isCurrent, request);
        return isCurrent;
    }

    @Transactional
    public void revokeOtherSessions(String username, String currentRefreshToken, HttpServletRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        String currentRefreshTokenHash = StringUtils.hasText(currentRefreshToken) ? hashToken(currentRefreshToken) : null;

        refreshTokenRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()).forEach(refreshToken -> {
            if (refreshToken.getRevokedAt() == null && !refreshToken.getTokenHash().equals(currentRefreshTokenHash)) {
                revokeRefreshTokenRecord(refreshToken, "OTHER_SESSIONS_REVOKED");
            }
        });
        authAuditService.record(EVENT_REVOKE_SESSION, user, user.getUsername(), true, "other sessions", request);
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

        Integer tokenAuthVersion = claims.getIntegerClaim(AUTH_VERSION_CLAIM);
        if ((tokenAuthVersion == null ? 0 : tokenAuthVersion) != authVersionOf(user)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

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
                .claim(AUTH_VERSION_CLAIM, authVersionOf(user))
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
                .claim(AUTH_VERSION_CLAIM, authVersionOf(user))
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
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hashToken(token))
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        validateRefreshTokenRecord(refreshToken, claims);
    }

    private void validateRefreshTokenRecord(RefreshToken refreshToken, JWTClaimsSet claims) {
        LocalDateTime now = LocalDateTime.now();

        if (!refreshToken.getTokenId().equals(claims.getJWTID())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (!refreshToken.isActive(now)) {
            if (refreshToken.getRevokedAt() != null) {
                revokeAllActiveRefreshTokensInNewTransaction(
                        refreshToken.getUser().getId(),
                        now,
                        "REUSE_DETECTED");
                log.warn("Refresh token reuse detected for user {}", refreshToken.getUser().getUsername());
            }
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    private int authVersionOf(User user) {
        return user.getAuthVersion() == null ? 0 : user.getAuthVersion();
    }

    private void revokeAllActiveRefreshTokensInNewTransaction(UUID userId, LocalDateTime revokedAt, String reason) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(status ->
                refreshTokenRepository.revokeAllActiveByUserId(userId, revokedAt, reason));
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
            revokeRefreshTokenRecord(refreshToken, reason);
        });
    }

    private void revokeRefreshTokenRecord(RefreshToken refreshToken, String reason) {
        if (refreshToken.getRevokedAt() == null) {
            refreshToken.setRevokedAt(LocalDateTime.now());
            refreshToken.setRevokedReason(reason);
            refreshTokenRepository.save(refreshToken);
        }
    }

    private User resolveUserFromRefreshToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        return refreshTokenRepository.findByTokenHash(hashToken(token))
                .map(RefreshToken::getUser)
                .orElse(null);
    }

    private AuthSessionResponse toSessionResponse(RefreshToken refreshToken, String currentRefreshTokenHash) {
        boolean current = StringUtils.hasText(currentRefreshTokenHash)
                && currentRefreshTokenHash.equals(refreshToken.getTokenHash());
        return AuthSessionResponse.builder()
                .id(refreshToken.getId())
                .current(current)
                .ipAddress(refreshToken.getIpAddress())
                .userAgent(refreshToken.getUserAgent())
                .createdAt(refreshToken.getCreatedAt())
                .expiresAt(refreshToken.getExpiresAt())
                .revokedAt(refreshToken.getRevokedAt())
                .revokedReason(refreshToken.getRevokedReason())
                .build();
    }

    private String normalizeUsername(String username) {
        return StringUtils.hasText(username) ? username.trim() : "";
    }

    private String loginRateLimitKey(String username, HttpServletRequest request) {
        return "login:" + extractClientIp(request) + ":" + username.toLowerCase(Locale.ROOT);
    }

    private String genericRateLimitKey(String prefix, HttpServletRequest request) {
        return prefix + ":" + extractClientIp(request);
    }

    private String resolveFailureReason(Exception exception) {
        if (exception instanceof AppException appException) {
            return appException.getErrorCode().name();
        }
        String message = exception.getMessage();
        return StringUtils.hasText(message) ? message : exception.getClass().getSimpleName();
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
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
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
