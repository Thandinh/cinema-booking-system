package com.cinema.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleRequest {

    @NotBlank(message = "ROLE_NAME_REQUIRED")
    @Size(min = 2, max = 50, message = "ROLE_NAME_INVALID")
    String name;

    String description;

    Set<UUID> permissionIds;
}
