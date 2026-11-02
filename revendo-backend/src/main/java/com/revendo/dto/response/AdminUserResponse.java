package com.revendo.dto.response;

import com.revendo.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Outgoing payload for a single row in the admin user list.
 * Requirements: 13.4
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserResponse {

    private Long          userId;
    private String        firstName;
    private String        lastName;
    private String        email;
    private User.Status   status;
    private LocalDateTime joinDate;
}
