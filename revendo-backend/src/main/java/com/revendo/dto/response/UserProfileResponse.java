package com.revendo.dto.response;

import com.revendo.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Outgoing payload for GET /api/users/me.
 * Requirements: 11.7
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private Long          userId;
    private String        firstName;
    private String        lastName;
    private String        email;
    private String        phoneNumber;
    private User.Role     role;
    private User.Status   status;
    private LocalDateTime createdAt;
}
