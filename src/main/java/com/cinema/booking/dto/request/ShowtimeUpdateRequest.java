package com.cinema.booking.dto.request;

import com.cinema.booking.enums.ShowtimeStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShowtimeUpdateRequest {

    @Future(message = "START_TIME_FUTURE")
    LocalDateTime startTime;

    @Future(message = "END_TIME_FUTURE")
    LocalDateTime endTime;

    @DecimalMin(value = "0.01", message = "BASE_PRICE_INVALID")
    BigDecimal basePrice;

    ShowtimeStatus status;
}
