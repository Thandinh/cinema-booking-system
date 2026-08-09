package com.cinema.booking.dto.request;

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
public class RefundCompleteRequest {

    @Size(max = 255, message = "PARAMETER_INVALID")
    String providerRefundId;

    @Size(max = 500, message = "PARAMETER_INVALID")
    String note;
}
