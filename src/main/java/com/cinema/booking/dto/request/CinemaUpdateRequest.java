package com.cinema.booking.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CinemaUpdateRequest {
    String name;
    String address;
    String city;
    Double latitude;
    Double longitude;
    Boolean isActive;
}
