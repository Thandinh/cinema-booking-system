package com.cinema.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TicketCheckInRequest {

    @NotBlank(message = "TICKET_QR_REQUIRED")
    @Size(max = 100, message = "INVALID_QR_CODE")
    String qrCode;
}
