package com.cinema.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CinemaResponse {
    UUID id;
    String name;
    String address;
    String city;
    Double latitude;
    Double longitude;
    Boolean isActive;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    
    // Optional: Included when fetching details
    List<RoomResponse> rooms;
}
