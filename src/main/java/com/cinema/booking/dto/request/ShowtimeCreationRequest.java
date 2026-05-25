package com.cinema.booking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
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
public class ShowtimeCreationRequest {

    @NotNull(message = "MOVIE_ID_REQUIRED")
    UUID movieId;

    @NotNull(message = "ROOM_ID_REQUIRED")
    UUID roomId;

    @NotNull(message = "START_TIME_REQUIRED")
    @Future(message = "START_TIME_FUTURE")
    LocalDateTime startTime;

    @NotNull(message = "END_TIME_REQUIRED")
    @Future(message = "END_TIME_FUTURE")
    LocalDateTime endTime;

    @NotNull(message = "BASE_PRICE_REQUIRED")
    @DecimalMin(value = "0.01", message = "BASE_PRICE_INVALID")
    BigDecimal basePrice;
}
