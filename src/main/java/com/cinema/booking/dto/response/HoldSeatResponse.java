package com.cinema.booking.dto.response;

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
public class HoldSeatResponse {
    UUID showtimeId;
    List<UUID> heldSeatIds;
    LocalDateTime holdUntil;
    BigDecimal estimatedTotalPrice;
    String message;
}
