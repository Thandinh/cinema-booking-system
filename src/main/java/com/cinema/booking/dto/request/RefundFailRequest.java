package com.cinema.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RefundFailRequest {

    @NotBlank(message = "REFUND_FAILURE_REASON_REQUIRED")
    @Size(max = 1000, message = "PARAMETER_INVALID")
    String failureReason;
}
