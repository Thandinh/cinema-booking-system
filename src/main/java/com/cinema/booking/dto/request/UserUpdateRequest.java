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
public class UserUpdateRequest {

    @Size(min = 6, max = 100, message = "PASSWORD_INVALID")
    String password;

    String firstName;

    String lastName;

    @Past(message = "DOB_INVALID")
    LocalDate dob;

    @Pattern(regexp = "^(\\+84|0)[3-9][0-9]{8}$", message = "PHONE_INVALID")
    String phone;

    @Email(message = "EMAIL_INVALID")
    String email;

    /** Chỉ ADMIN mới được phép cập nhật roles */
    Set<UUID> roleIds;
}
