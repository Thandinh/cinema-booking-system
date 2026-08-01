package com.cinema.booking.dto.response;

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
public class SeatResponse {
    UUID id;
    UUID roomId;
    String roomName;       // denormalized
    String rowLabel;
    Integer seatNumber;
    String seatCode;       // e.g. "A5" — computed from rowLabel + seatNumber
    SeatType seatType;
    BigDecimal priceMultiplier;
    Integer rowIndex;
    Integer colIndex;
}
