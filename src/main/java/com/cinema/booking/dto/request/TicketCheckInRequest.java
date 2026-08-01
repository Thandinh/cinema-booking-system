package com.cinema.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TicketCheckInRequest {

    @NotBlank(message = "TICKET_QR_REQUIRED")
    @Size(max = 100, message = "INVALID_QR_CODE")
    String qrCode;

    @NotNull(message = "TICKET_CHECKIN_CONTEXT_REQUIRED")
    UUID cinemaId;

    @NotNull(message = "TICKET_CHECKIN_CONTEXT_REQUIRED")
    UUID showtimeId;
}
