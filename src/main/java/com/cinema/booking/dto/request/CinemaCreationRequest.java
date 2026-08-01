package com.cinema.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CinemaCreationRequest {
    @NotBlank(message = "CINEMA_NAME_REQUIRED")
    String name;
    String address;
    String city;
    Double latitude;
    Double longitude;
}
