package com.cinema.booking.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HoldSeatRequest {

    @NotNull(message = "SHOWTIME_ID_REQUIRED")
    UUID showtimeId;

    @NotEmpty(message = "SEAT_IDS_REQUIRED")
    @Size(min = 1, max = 10, message = "SEAT_IDS_SIZE_INVALID")
    List<UUID> seatIds;
}
