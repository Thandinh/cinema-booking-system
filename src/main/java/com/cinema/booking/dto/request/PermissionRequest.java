package com.cinema.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionRequest {

    @NotBlank(message = "PERMISSION_NAME_REQUIRED")
    @Size(min = 2, max = 100, message = "PERMISSION_NAME_INVALID")
    String name;

    String description;
}
