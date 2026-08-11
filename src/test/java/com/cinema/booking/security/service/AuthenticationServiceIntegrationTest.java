package com.cinema.booking.security.service;

import com.cinema.booking.dto.request.AuthenticationRequest;
import com.cinema.booking.dto.request.ChangePasswordRequest;
import com.cinema.booking.dto.request.IntrospectRequest;
import com.cinema.booking.dto.request.LogoutRequest;
import com.cinema.booking.dto.request.RefreshRequest;
import com.cinema.booking.dto.response.AuthenticationResponse;
import com.cinema.booking.entity.RefreshToken;
import com.cinema.booking.entity.User;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.repository.AuthAuditLogRepository;
import com.cinema.booking.repository.InvalidatedTokenRepository;
import com.cinema.booking.repository.RefreshTokenRepository;
import com.cinema.booking.repository.UserRepository;
import com.cinema.booking.support.PostgresIntegrationTest;
import com.cinema.booking.service.UserService;
import com.nimbusds.jose.JOSEException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "jwt.signer-key=test-auth-secret-key-with-more-than-sixty-four-characters-for-hs512-signing",
        "jwt.access-token-valid-duration=3600",
        "jwt.refresh-token-valid-duration=604800"
})
class AuthenticationServiceIntegrationTest extends PostgresIntegrationTest {

    private static final String RAW_PASSWORD = "P@ssword1!";

    @Autowired
    AuthenticationService authenticationService;

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Autowired
    InvalidatedTokenRepository invalidatedTokenRepository;

    @Autowired
    AuthAuditLogRepository authAuditLogRepository;

    @BeforeEach
    void setUp() {
        clearAuthData();
    }

    @AfterEach
    void tearDown() {
        clearAuthData();
    }

    @Test
    void authenticate_shouldRejectUsersWhoseEmailIsNotVerified() {
        User user = createUser(false);

        assertThatThrownBy(() -> authenticationService.authenticate(
                AuthenticationRequest.builder()
                        .username(user.getUsername())
                        .password(RAW_PASSWORD)
                        .build(),
                request()))
                .isInstanceOfSatisfying(AppException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED));

        assertThat(refreshTokenRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())).isEmpty();
        assertThat(authAuditLogRepository.findAll())
                .anySatisfy(log -> {
                    assertThat(log.getUsername()).isEqualTo(user.getUsername());
                    assertThat(log.getEventType()).isEqualTo(AuthenticationService.EVENT_LOGIN_PASSWORD);
                    assertThat(log.getSuccess()).isFalse();
                    assertThat(log.getFailureReason()).isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED.name());
                });
    }

    @Test
    void refreshToken_shouldRotateRefreshTokenAndRejectReuse() throws ParseException, JOSEException {
        User user = createUser(true);
        AuthenticationResponse loginResponse = authenticationService.authenticate(
                AuthenticationRequest.builder()
                        .username(user.getUsername())
                        .password(RAW_PASSWORD)
                        .build(),
                request());

        AuthenticationResponse refreshedResponse = authenticationService.refreshToken(
                RefreshRequest.builder()
                        .token(loginResponse.getRefreshToken())
                        .build(),
                request());

        assertThat(refreshedResponse.getRefreshToken()).isNotEqualTo(loginResponse.getRefreshToken());
        List<RefreshToken> tokensAfterRotation = refreshTokenRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
        assertThat(tokensAfterRotation).hasSize(2);
        assertThat(tokensAfterRotation)
                .anySatisfy(refreshToken -> {
                    assertThat(refreshToken.getRevokedAt()).isNotNull();
                    assertThat(refreshToken.getRevokedReason()).isEqualTo("ROTATED");
                    assertThat(refreshToken.getReplacedByTokenId()).isNotBlank();
                })
                .anySatisfy(refreshToken -> {
                    assertThat(refreshToken.getRevokedAt()).isNull();
                    assertThat(refreshToken.getExpiresAt()).isAfter(LocalDateTime.now());
                });

        assertThatThrownBy(() -> authenticationService.refreshToken(
                RefreshRequest.builder()
                        .token(loginResponse.getRefreshToken())
                        .build(),
                request()))
                .isInstanceOfSatisfying(AppException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));

        assertThat(refreshTokenRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()))
                .allSatisfy(refreshToken -> assertThat(refreshToken.getRevokedAt()).isNotNull());
    }

    @Test
    void logout_shouldInvalidateAccessTokenAndRevokeRefreshToken() throws ParseException, JOSEException {
        User user = createUser(true);
        AuthenticationResponse loginResponse = authenticationService.authenticate(
                AuthenticationRequest.builder()
                        .username(user.getUsername())
                        .password(RAW_PASSWORD)
                        .build(),
                request());

        authenticationService.logout(LogoutRequest.builder()
                .token(loginResponse.getAccessToken())
                .refreshToken(loginResponse.getRefreshToken())
                .build(), request());

        assertThat(authenticationService.introspect(
                IntrospectRequest.builder()
                        .token(loginResponse.getAccessToken())
                        .build()).isValid())
                .isFalse();
        assertThat(refreshTokenRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()))
                .singleElement()
                .satisfies(refreshToken -> {
                    assertThat(refreshToken.getRevokedAt()).isNotNull();
                    assertThat(refreshToken.getRevokedReason()).isEqualTo("LOGOUT");
                });
        assertThat(invalidatedTokenRepository.count()).isEqualTo(1);
    }

    @Test
    void changePassword_shouldInvalidateExistingAccessAndRefreshTokens() throws ParseException, JOSEException {
        User user = createUser(true);
        AuthenticationResponse loginResponse = authenticationService.authenticate(
                AuthenticationRequest.builder()
                        .username(user.getUsername())
                        .password(RAW_PASSWORD)
                        .build(),
                request());

        userService.changeMyPassword(user.getUsername(), ChangePasswordRequest.builder()
                .currentPassword(RAW_PASSWORD)
                .newPassword("N3wP@ssword!")
                .confirmPassword("N3wP@ssword!")
                .build());

        assertThat(authenticationService.introspect(IntrospectRequest.builder()
                .token(loginResponse.getAccessToken())
                .build()).isValid()).isFalse();
        assertThatThrownBy(() -> authenticationService.refreshToken(RefreshRequest.builder()
                .token(loginResponse.getRefreshToken())
                .build(), request()))
                .isInstanceOfSatisfying(AppException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));
        assertThat(refreshTokenRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()))
                .singleElement()
                .satisfies(refreshToken -> {
                    assertThat(refreshToken.getRevokedAt()).isNotNull();
                    assertThat(refreshToken.getRevokedReason()).isEqualTo("PASSWORD_CHANGED");
                });
    }

    private void clearAuthData() {
        invalidatedTokenRepository.deleteAllInBatch();
        refreshTokenRepository.deleteAllInBatch();
        authAuditLogRepository.deleteAllInBatch();
    }

    private User createUser(boolean emailVerified) {
        String suffix = UUID.randomUUID().toString();
        return userRepository.save(User.builder()
                .username("auth_test_" + suffix)
                .password(new BCryptPasswordEncoder(10).encode(RAW_PASSWORD))
                .firstName("Auth")
                .lastName("Test")
                .email("auth-test-" + suffix + "@cinema.test")
                .emailVerified(emailVerified)
                .isActive(true)
                .isDeleted(false)
                .build());
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "JUnit");
        return request;
    }
}
