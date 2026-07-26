package com.cinema.booking.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {

    @NotBlank(message = "USERNAME_REQUIRED")
    @Size(min = 4, max = 50, message = "USERNAME_INVALID")
    String username;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 8, max = 72, message = "PASSWORD_INVALID")
    @Pattern(
            regexp = "^(?=\\S+$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).+$",
            message = "PASSWORD_INVALID"
    )
    String password;

    String firstName;

    String lastName;

    @Past(message = "DOB_INVALID")
    LocalDate dob;

    @Pattern(regexp = "^(\\+84|0)[3-9][0-9]{8}$", message = "PHONE_INVALID")
    String phone;

    @NotBlank(message = "EMAIL_REQUIRED")
    @Email(message = "EMAIL_INVALID")
    String email;

    @Size(max = 500, message = "AVATAR_URL_INVALID")
    String avatarUrl;

    /** Only admin-created users can receive roles. Public registration ignores this field. */
    Set<UUID> roleIds;

    /** Cinema scope for STAFF users. Ignored by public registration. */
    Set<UUID> assignedCinemaIds;
}
