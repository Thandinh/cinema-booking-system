package com.cinema.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    UUID id;
    String username;
    String firstName;
    String lastName;
    LocalDate dob;
    String phone;
    String email;
    String avatarUrl;
    Boolean emailVerified;
    Boolean isActive;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Set<RoleResponse> roles;
}
