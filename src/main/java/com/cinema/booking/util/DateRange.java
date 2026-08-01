package com.cinema.booking.util;

import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.exception.AppException;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DateRange(LocalDateTime fromInclusive, LocalDateTime toExclusive) {

    private static final LocalDateTime MIN_SEARCH_TIME = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime MAX_SEARCH_TIME = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    public static DateRange of(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new AppException(ErrorCode.DATE_RANGE_INVALID);
        }

        return new DateRange(
                fromDate == null ? null : fromDate.atStartOfDay(),
                toDate == null ? null : toDate.plusDays(1).atStartOfDay());
    }

    public LocalDateTime fromSearchBound() {
        return fromInclusive == null ? MIN_SEARCH_TIME : fromInclusive;
    }

    public LocalDateTime toSearchBound() {
        return toExclusive == null ? MAX_SEARCH_TIME : toExclusive;
    }
}
