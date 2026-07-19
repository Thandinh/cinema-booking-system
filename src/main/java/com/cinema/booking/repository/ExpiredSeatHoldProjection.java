package com.cinema.booking.repository;

import java.util.UUID;

public interface ExpiredSeatHoldProjection {
    UUID getId();

    UUID getShowtimeId();

    UUID getSeatId();
}
