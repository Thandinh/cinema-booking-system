package com.cinema.booking.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    // ── Generic ──────────────────────────────────────────────────────────────
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error",                  HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY            (1001, "Invalid message key",                  HttpStatus.BAD_REQUEST),

    // ── User ─────────────────────────────────────────────────────────────────
    USER_EXISTED           (1002, "Username already exists",              HttpStatus.CONFLICT),
    EMAIL_EXISTED          (1003, "Email already exists",                 HttpStatus.CONFLICT),
    ROLE_NOT_FOUND         (1004, "Role not found",                       HttpStatus.NOT_FOUND),
    USER_NOT_FOUND         (1005, "User not found",                       HttpStatus.NOT_FOUND),
    USER_NOT_EXISTED       (1005, "User not found",                       HttpStatus.NOT_FOUND),  // backward-compat alias
    UNAUTHENTICATED        (1006, "Unauthenticated",                      HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED           (1007, "You do not have permission",           HttpStatus.FORBIDDEN),
    USER_NOT_ACTIVE        (1008, "User account is inactive or blocked",  HttpStatus.FORBIDDEN),
    CANNOT_BLOCK_SELF      (1009, "Cannot block your own account",        HttpStatus.BAD_REQUEST),

    // ── Validation keys — matched by GlobalExceptionHandler via Enum.valueOf() ─
    USERNAME_INVALID       (1010, "Username must be between {min} and 50 characters", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID       (1011, "Password must be at least {min} characters",       HttpStatus.BAD_REQUEST),
    EMAIL_INVALID          (1012, "Invalid email format",                             HttpStatus.BAD_REQUEST),
    PHONE_INVALID          (1013, "Invalid Vietnamese phone number",                  HttpStatus.BAD_REQUEST),
    DOB_INVALID            (1014, "Date of birth must be in the past",               HttpStatus.BAD_REQUEST),
    USERNAME_REQUIRED      (1015, "Username is required",                             HttpStatus.BAD_REQUEST),
    PASSWORD_REQUIRED      (1016, "Password is required",                             HttpStatus.BAD_REQUEST),
    FIRSTNAME_REQUIRED     (1017, "First name is required",                           HttpStatus.BAD_REQUEST),
    LASTNAME_REQUIRED      (1018, "Last name is required",                            HttpStatus.BAD_REQUEST),
    EMAIL_REQUIRED         (1019, "Email is required",                                HttpStatus.BAD_REQUEST),
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code       = code;
        this.message    = message;
        this.statusCode = statusCode;
    }

    private final int           code;
    private final String        message;
    private final HttpStatusCode statusCode;
}
