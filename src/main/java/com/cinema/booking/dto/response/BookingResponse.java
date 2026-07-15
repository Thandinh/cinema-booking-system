package com.cinema.booking.dto.response;

import com.cinema.booking.enums.BookingStatus;
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

    // Showtime info
    UUID showtimeId;
    String movieTitle;
    String cinemaName;
    String cinemaAddress;
    String cinemaCity;
    String roomName;
    LocalDateTime startTime;

    // Financial
    BigDecimal totalPrice;
    BigDecimal discountAmount;

    // Promotion info (nullable)
    String promotionCode;

    List<BookingDetailResponse> bookingDetails;

    LocalDateTime createdAt;
}
