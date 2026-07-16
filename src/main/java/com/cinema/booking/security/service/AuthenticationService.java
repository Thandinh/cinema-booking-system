package com.cinema.booking.security.service;

import com.cinema.booking.configuration.JwtProperties;
import com.cinema.booking.configuration.GoogleOAuthProperties;
import com.cinema.booking.dto.request.AuthenticationRequest;
import com.cinema.booking.dto.request.GoogleLoginRequest;
import com.cinema.booking.dto.request.IntrospectRequest;
import com.cinema.booking.dto.request.LogoutRequest;
import com.cinema.booking.dto.request.RefreshRequest;
import com.cinema.booking.dto.response.AuthenticationResponse;
import com.cinema.booking.dto.response.IntrospectResponse;
import com.cinema.booking.entity.InvalidatedToken;
import com.cinema.booking.entity.Role;
import com.cinema.booking.entity.User;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.repository.InvalidatedTokenRepository;
import com.cinema.booking.repository.RoleRepository;
import com.cinema.booking.repository.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
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

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    UserRepository userRepository;
    RoleRepository roleRepository;
    InvalidatedTokenRepository invalidatedTokenRepository;
    JwtProperties jwtProperties;
    GoogleOAuthProperties googleOAuthProperties;

    static final String GOOGLE_ISSUER = "https://accounts.google.com";
    static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";

    public IntrospectResponse introspect(IntrospectRequest request) {
        var token = request.getToken();
        boolean isValid = true;
        try {
            verifyToken(token, false);
        } catch (AppException | JOSEException | ParseException e) {
            isValid = false;
        }
        return IntrospectResponse.builder().valid(isValid).build();
    }

    public String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("cinema-booking")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(jwtProperties.getAccessTokenValidDuration(), ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString()) // Tạo ID định danh cho token
                .claim("scope", buildScope(user))
                .claim("userId", user.getId().toString())
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(jwtProperties.getSignerKey().getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Error signing token", e);
        }
    }

    public SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(jwtProperties.getSignerKey().getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expiryTime = isRefresh
                ? new Date(signedJWT.getJWTClaimsSet().getIssueTime().toInstant()
                .plus(jwtProperties.getRefreshTokenValidDuration(), ChronoUnit.SECONDS).toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();

        var verified = signedJWT.verify(verifier);

        if (!(verified && expiryTime.after(new Date())))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        if (invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        var username = signedJWT.getJWTClaimsSet().getSubject();
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        validateUserCanAuthenticate(user);

        return signedJWT;
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        // 1. Tìm user theo username
        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        validateUserCanAuthenticate(user);

        // 2. Kiểm tra mật khẩu (Sử dụng PasswordEncoder đã inject)
        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!authenticated) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // 3. Nếu đúng mật khẩu, tiến hành tạo Token
        var token = generateToken(user);

        // 4. Trả về Response
        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }

    @Transactional
    public AuthenticationResponse authenticateWithGoogle(GoogleLoginRequest request) {
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

        String token = generateToken(user);
        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }

    public AuthenticationResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException {
        // 1. Kiểm tra tính hợp lệ của token cũ (Check chữ ký, thời hạn refresh và blacklist)
        // isRefresh = true để hàm verifyToken kiểm tra theo thời hạn Refreshable Duration
        var signedJWT = verifyToken(request.getToken(), true);

        // 2. Thu hồi token cũ (Vô hiệu hóa ngay lập tức token vừa dùng để refresh)
        var jit = signedJWT.getJWTClaimsSet().getJWTID();
        var expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .id(jit)
                .expiryTime(expiryTime)
                .build();

        invalidatedTokenRepository.save(invalidatedToken);

        // 3. Lấy thông tin user từ token cũ để cấp token mới
        var username = signedJWT.getJWTClaimsSet().getSubject();
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        // 4. Tạo token mới "chính chủ" cho hệ thống Cinema
        var newToken = generateToken(user);

        return AuthenticationResponse.builder()
                .token(newToken)
                .authenticated(true)
                .build();
    }

    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        try {
            var signedToken = verifyToken(request.getToken(), true);
            String jit = signedToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signedToken.getJWTClaimsSet().getExpirationTime();

            InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                    .id(jit)
                    .expiryTime(expiryTime)
                    .build();

            invalidatedTokenRepository.save(invalidatedToken);
        } catch (AppException e) {
            log.info("Token already expired or invalid");
        }
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
                stringJoiner.add("ROLE_" + role.getName()); // Prefix ROLE_ cho Spring Security
                if (!CollectionUtils.isEmpty(role.getPermissions())) {
                    role.getPermissions().forEach(p -> stringJoiner.add(p.getName()));
                }
            });
        }
        return stringJoiner.toString();
    }
}
