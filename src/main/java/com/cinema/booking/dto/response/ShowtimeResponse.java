package com.cinema.booking.dto.response;

import com.cinema.booking.enums.ShowtimeStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShowtimeResponse {
    UUID id;
    
    // Movie Info
    UUID movieId;
    String movieTitle;
    String moviePosterUrl;
    Integer movieDuration;
    
    // Room Info
    UUID roomId;
    String roomName;
    
    // Cinema Info
    UUID cinemaId;
    String cinemaName;
    String cinemaAddress;
    String cinemaCity;

    LocalDateTime startTime;
    LocalDateTime endTime;
    BigDecimal basePrice;
    ShowtimeStatus status;
}
