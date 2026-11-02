package com.revendo.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Incoming payload for POST /api/auth/register.
 *
 * Bean Validation annotations enforce structure; business rules
 * (email uniqueness, phone digit count, password confirmation) are
 * checked in {@link com.revendo.service.AuthService}.
 *
 * Requirements: 2.1–2.10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class RegisterRequest {

    @NotBlank(message = "First name is required.")
    @Size(max = 50, message = "First name must not exceed 50 characters.")
    private String firstName;

    @NotBlank(message = "Last name is required.")
    @Size(max = 50, message = "Last name must not exceed 50 characters.")
    private String lastName;

    @NotBlank(message = "Email address is required.")
    @Email(message = "Email address format is invalid.")
    @Size(max = 254, message = "Email address must not exceed 254 characters.")
    private String email;

    @NotBlank(message = "Phone number is required.")
    @Size(max = 15, message = "Phone number must not exceed 15 characters.")
    private String phoneNumber;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 128,
          message = "Password must be between 8 and 128 characters.")
    private String password;

    @NotBlank(message = "Confirm password is required.")
    private String confirmPassword;
}
