package com.cinema.booking.dto.response;

import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.enums.ShowtimeStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingResponse {
    UUID id;
    String secureToken;
    BookingStatus status;

    UUID customerId;
    String customerUsername;
    String customerName;
    String customerEmail;

    // Showtime info
    UUID showtimeId;
    ShowtimeStatus showtimeStatus;
    String movieTitle;
    String cinemaName;
    String cinemaAddress;
    String cinemaCity;
    String roomName;
    LocalDateTime startTime;
    LocalDateTime paymentExpiresAt;

    // Financial
    BigDecimal totalPrice;
    BigDecimal discountAmount;

    // Promotion info (nullable)
    String promotionCode;

    List<BookingDetailResponse> bookingDetails;

    LocalDateTime createdAt;
}
