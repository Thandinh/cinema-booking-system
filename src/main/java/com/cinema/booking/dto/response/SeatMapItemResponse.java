package com.cinema.booking.dto.response;

import com.cinema.booking.enums.SeatStatusType;
import com.cinema.booking.enums.SeatType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeatMapItemResponse {
    UUID seatStatusId;
    UUID seatId;
    String rowLabel;
    Integer seatNumber;
    SeatType seatType;
    Integer rowIndex;
    Integer colIndex;
    BigDecimal priceMultiplier;
    SeatStatusType status; // AVAILABLE, HOLD, BOOKED
}
