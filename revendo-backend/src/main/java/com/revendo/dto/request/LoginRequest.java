package com.revendo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Incoming payload for POST /api/auth/login.
 *
 * Requirements: 3.1, 3.4
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "Email address is required.")
    @Size(max = 254, message = "Email address must not exceed 254 characters.")
    private String email;

    @NotBlank(message = "Password is required.")
    @Size(max = 128, message = "Password must not exceed 128 characters.")
    private String password;
}
