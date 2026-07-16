package com.cinema.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GoogleLoginRequest {

    @NotBlank(message = "GOOGLE_ID_TOKEN_REQUIRED")
    String idToken;
}
