package com.cinema.booking.exception;

import com.cinema.booking.enums.ErrorCode;

/**
 * Signals an expired checkout after its release work has completed.
 *
 * The payment initiation transaction must commit the cleanup before the API
 * returns the normal booking-expired response to the client.
 */
public class BookingExpiredAfterCleanupException extends AppException {

    public BookingExpiredAfterCleanupException() {
        super(ErrorCode.BOOKING_EXPIRED);
    }
}
