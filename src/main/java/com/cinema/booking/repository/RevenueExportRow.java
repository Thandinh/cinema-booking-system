package com.cinema.booking.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface RevenueExportRow {
    LocalDateTime getPaymentTime();
    String getTransactionNo();
    String getPaymentMethod();
    BigDecimal getAmount();
    UUID getBookingId();
    String getBookingStatus();
    String getUsername();
    String getEmail();
    String getMovieTitle();
    String getCinemaName();
    String getCinemaCity();
    String getRoomName();
    LocalDateTime getShowtimeStartTime();
    Long getTicketCount();
    String getSeats();
}
