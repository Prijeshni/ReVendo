package com.revendo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Outgoing payload for a successful POST /api/auth/login.
 *
 * Requirements: 3.2, 3.5, 3.6
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    /** The signed JWT the client stores in localStorage. */
    private String token;

    /** The user's role string, e.g. "ROLE_USER" or "ROLE_ADMIN". */
    private String role;

    /** The user's numeric database ID. */
    private Long userId;

    /** First name + last name for display in the navigation bar. */
    private String displayName;
}
