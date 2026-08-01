package com.cinema.booking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequest {

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

    @Email(message = "EMAIL_INVALID")
    String email;

    @Size(max = 500, message = "AVATAR_URL_INVALID")
    String avatarUrl;

    /** Only ADMIN can update roles. Null means keep current roles. */
    Set<UUID> roleIds;

    /** Only ADMIN can update STAFF cinema scope. Null means keep current assignments. */
    Set<UUID> assignedCinemaIds;
}
