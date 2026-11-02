package com.revendo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Outgoing payload for a single notification.
 * Requirements: 11 (notifications section)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long          notificationId;
    private String        message;
    private boolean       isRead;
    private LocalDateTime createdAt;
}
