package com.cinema.booking.dto.request;

import com.cinema.booking.enums.SeatType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeatCreationRequest {

    @NotNull(message = "ROOM_ID_REQUIRED")
    UUID roomId;

    @NotBlank(message = "SEAT_ROW_REQUIRED")
    String rowLabel;

    @NotNull(message = "SEAT_NUMBER_REQUIRED")
    @Min(value = 1, message = "SEAT_NUMBER_INVALID")
    Integer seatNumber;

    @Builder.Default
    SeatType seatType = SeatType.NORMAL;

    @DecimalMin(value = "0.0", message = "SEAT_MULTIPLIER_INVALID")
    @DecimalMax(value = "99.99", message = "SEAT_MULTIPLIER_INVALID")
    @Builder.Default
    BigDecimal priceMultiplier = BigDecimal.ONE;

    Integer rowIndex;
    Integer colIndex;
}
