package com.cinema.booking.dto.request;

import com.cinema.booking.enums.SeatType;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeatUpdateRequest {

    SeatType seatType;

    @DecimalMin(value = "0.0", message = "SEAT_MULTIPLIER_INVALID")
    BigDecimal priceMultiplier;

    Integer rowIndex;
    Integer colIndex;
}
