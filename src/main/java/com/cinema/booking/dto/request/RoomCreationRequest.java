package com.cinema.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomCreationRequest {
    @NotNull(message = "CINEMA_ID_REQUIRED")
    UUID cinemaId;
    
    @NotBlank(message = "ROOM_NAME_REQUIRED")
    String name;
}
