package com.cinema.booking.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

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
    @Size(min = 6, max = 100, message = "PASSWORD_INVALID")
    String password;

    @NotBlank(message = "FIRSTNAME_REQUIRED")
    String firstName;

    @NotBlank(message = "LASTNAME_REQUIRED")
    String lastName;

    @Past(message = "DOB_INVALID")
    LocalDate dob;

    @Pattern(regexp = "^(\\+84|0)[3-9][0-9]{8}$", message = "PHONE_INVALID")
    String phone;

    @NotBlank(message = "EMAIL_REQUIRED")
    @Email(message = "EMAIL_INVALID")
    String email;
}
