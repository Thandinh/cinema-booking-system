package com.cinema.booking.controller;

import com.cinema.booking.dto.request.AuthenticationRequest;
import com.cinema.booking.dto.request.GoogleLoginRequest;
import com.cinema.booking.dto.request.IntrospectRequest;
import com.cinema.booking.dto.request.LogoutRequest;
import com.cinema.booking.dto.request.RefreshRequest;
import com.cinema.booking.dto.response.ApiResponse;
import com.cinema.booking.dto.response.AuthenticationResponse;
import com.cinema.booking.dto.response.IntrospectResponse;
import com.cinema.booking.security.service.AuthenticationService;
import com.nimbusds.jose.JOSEException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.time.Duration;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    private static final String REFRESH_COOKIE_NAME = "cinema_refresh_token";

    AuthenticationService authenticationService;

    @PostMapping("/token")
    ApiResponse<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        var result = authenticationService.authenticate(request, servletRequest);
        writeRefreshCookie(servletResponse, servletRequest, result.getRefreshToken(), result.getRefreshExpiresIn());
        return ApiResponse.<AuthenticationResponse>builder().result(hideRefreshToken(result)).build();
    }

    @PostMapping("/google")
    ApiResponse<AuthenticationResponse> authenticateWithGoogle(
            @RequestBody GoogleLoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        var result = authenticationService.authenticateWithGoogle(request, servletRequest);
        writeRefreshCookie(servletResponse, servletRequest, result.getRefreshToken(), result.getRefreshExpiresIn());
        return ApiResponse.<AuthenticationResponse>builder().result(hideRefreshToken(result)).build();
    }

    @PostMapping("/introspect")
    ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request) {
        var result = authenticationService.introspect(request);
        return ApiResponse.<IntrospectResponse>builder().result(result).build();
    }


    @PostMapping("/refresh")
    ApiResponse<AuthenticationResponse> authenticate(
            @RequestBody(required = false) RefreshRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse)
            throws ParseException, JOSEException {
        RefreshRequest resolvedRequest = request == null ? new RefreshRequest() : request;
        if (!StringUtils.hasText(resolvedRequest.getToken())) {
            resolvedRequest.setToken(readRefreshCookie(servletRequest));
        }
        var result = authenticationService.refreshToken(resolvedRequest, servletRequest);
        writeRefreshCookie(servletResponse, servletRequest, result.getRefreshToken(), result.getRefreshExpiresIn());
        return ApiResponse.<AuthenticationResponse>builder().result(hideRefreshToken(result)).build();
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(
            @RequestBody(required = false) LogoutRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) throws ParseException, JOSEException {
        LogoutRequest resolvedRequest = request == null ? new LogoutRequest() : request;
        if (!StringUtils.hasText(resolvedRequest.getRefreshToken())) {
            resolvedRequest.setRefreshToken(readRefreshCookie(servletRequest));
        }
        authenticationService.logout(resolvedRequest);
        clearRefreshCookie(servletResponse, servletRequest);
        return ApiResponse.<Void>builder().build();
    }

    private void writeRefreshCookie(
            HttpServletResponse response,
            HttpServletRequest request,
            String refreshToken,
            long maxAgeSeconds) {
        if (!StringUtils.hasText(refreshToken)) return;

        ResponseCookie cookie = refreshCookieBuilder(request)
                .value(refreshToken)
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private AuthenticationResponse hideRefreshToken(AuthenticationResponse result) {
        result.setRefreshToken(null);
        return result;
    }

    private void clearRefreshCookie(HttpServletResponse response, HttpServletRequest request) {
        ResponseCookie cookie = refreshCookieBuilder(request)
                .value("")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private ResponseCookie.ResponseCookieBuilder refreshCookieBuilder(HttpServletRequest request) {
        boolean secure = isSecureRequest(request);
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(secure ? "None" : "Lax")
                .path("/auth");
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return request.isSecure() || "https".equalsIgnoreCase(forwardedProto);
    }

    private String readRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (REFRESH_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
