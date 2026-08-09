package com.cinema.booking.exception;

import com.cinema.booking.enums.ErrorCode;
import lombok.Getter;

@Getter
public class RateLimitExceededException extends AppException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(ErrorCode errorCode, long retryAfterSeconds) {
        super(errorCode);
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }
}
