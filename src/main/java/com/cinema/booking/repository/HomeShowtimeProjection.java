package com.cinema.booking.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface HomeShowtimeProjection {

    UUID getShowtimeId();

    UUID getMovieId();

    String getMovieTitle();

    String getPosterUrl();

    String getAgeRating();

    Integer getMovieDuration();

    UUID getCinemaId();

    String getCinemaName();

    UUID getRoomId();

    String getRoomName();

    LocalDateTime getStartTime();

    LocalDateTime getEndTime();

    BigDecimal getBasePrice();
}
