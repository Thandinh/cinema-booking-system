package com.cinema.booking.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HomeShowtimeItemResponse {

    UUID id;
    UUID cinemaId;
    String cinemaName;
    UUID roomId;
    String roomName;
    LocalDateTime startTime;
    LocalDateTime endTime;
    BigDecimal basePrice;
}
