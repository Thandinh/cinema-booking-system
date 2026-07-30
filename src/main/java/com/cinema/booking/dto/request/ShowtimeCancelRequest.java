package com.cinema.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShowtimeCancelRequest {

    @NotBlank(message = "SHOWTIME_CANCEL_REASON_REQUIRED")
    @Size(max = 500, message = "SHOWTIME_CANCEL_REASON_INVALID")
    String reason;
}
