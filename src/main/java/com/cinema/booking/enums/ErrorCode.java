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

    // ── Movie ────────────────────────────────────────────────────────────────
    MOVIE_NOT_FOUND        (2001, "Movie not found",                      HttpStatus.NOT_FOUND),
    MOVIE_TITLE_EXISTED    (2002, "Movie title already exists",           HttpStatus.CONFLICT),
    MOVIE_TITLE_REQUIRED   (2010, "Movie title is required",              HttpStatus.BAD_REQUEST),
    MOVIE_DURATION_REQUIRED(2011, "Movie duration is required",           HttpStatus.BAD_REQUEST),
    MOVIE_DURATION_INVALID (2012, "Movie duration must be greater than 0",HttpStatus.BAD_REQUEST),
    MOVIE_RELEASE_DATE_REQUIRED(2013, "Movie release date is required",   HttpStatus.BAD_REQUEST),
    MOVIE_STATUS_REQUIRED  (2014, "Movie status is required",             HttpStatus.BAD_REQUEST),

    // ── Cinema & Room ────────────────────────────────────────────────────────
    CINEMA_NOT_FOUND       (3001, "Cinema not found",                     HttpStatus.NOT_FOUND),
    CINEMA_NAME_EXISTED    (3002, "Cinema name already exists",           HttpStatus.CONFLICT),
    ROOM_NOT_FOUND         (3003, "Room not found",                       HttpStatus.NOT_FOUND),
    ROOM_NAME_EXISTED      (3004, "Room name already exists in this cinema", HttpStatus.CONFLICT),
    CINEMA_NAME_REQUIRED   (3010, "Cinema name is required",              HttpStatus.BAD_REQUEST),
    ROOM_NAME_REQUIRED     (3011, "Room name is required",                HttpStatus.BAD_REQUEST),
    CINEMA_ID_REQUIRED     (3012, "Cinema ID is required",                HttpStatus.BAD_REQUEST),
    ROOM_ID_REQUIRED       (3013, "Room ID is required",                  HttpStatus.BAD_REQUEST),

    // ── Seat ────────────────────────────────────────────────────────────────
    SEAT_NOT_FOUND         (4001, "Seat not found",                       HttpStatus.NOT_FOUND),
    SEAT_ALREADY_EXISTS    (4002, "Seat already exists in this room",     HttpStatus.CONFLICT),
    SEAT_IN_USE            (4003, "Seat is currently in use (booked/held)",HttpStatus.CONFLICT),
    SEAT_ROW_REQUIRED      (4010, "Seat row label is required",           HttpStatus.BAD_REQUEST),
    SEAT_NUMBER_REQUIRED   (4011, "Seat number is required",              HttpStatus.BAD_REQUEST),
    SEAT_NUMBER_INVALID    (4012, "Seat number must be >= 1",             HttpStatus.BAD_REQUEST),
    SEAT_MULTIPLIER_INVALID(4013, "Price multiplier must be >= 0",        HttpStatus.BAD_REQUEST),
    SEAT_ROW_SIZE_INVALID  (4014, "Row labels must have 1-26 entries",    HttpStatus.BAD_REQUEST),

    // ── Showtime ────────────────────────────────────────────────────────────
    SHOWTIME_NOT_FOUND         (5001, "Showtime not found",                    HttpStatus.NOT_FOUND),
    SHOWTIME_TIME_OVERLAPPING  (5002, "Time overlaps with an existing showtime in this room", HttpStatus.CONFLICT),
    SHOWTIME_END_TIME_INVALID  (5003, "End time must be after start time",     HttpStatus.BAD_REQUEST),
    MOVIE_ID_REQUIRED          (5010, "Movie ID is required",                  HttpStatus.BAD_REQUEST),
    START_TIME_REQUIRED        (5011, "Start time is required",                HttpStatus.BAD_REQUEST),
    END_TIME_REQUIRED          (5012, "End time is required",                  HttpStatus.BAD_REQUEST),
    BASE_PRICE_REQUIRED        (5013, "Base price is required",                HttpStatus.BAD_REQUEST),
    START_TIME_FUTURE          (5014, "Start time must be in the future",      HttpStatus.BAD_REQUEST),
    END_TIME_FUTURE            (5015, "End time must be in the future",        HttpStatus.BAD_REQUEST),
    BASE_PRICE_INVALID         (5016, "Base price must be valid",              HttpStatus.BAD_REQUEST),
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
