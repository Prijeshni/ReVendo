package com.revendo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Outgoing payload for a single message inside a conversation.
 *
 * Requirements: 9.4, 9.6
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {

    private Long   messageId;
    private Long   conversationId;
    private Long   senderId;
    private String senderDisplayName;
    private String text;
    private boolean isRead;
    private LocalDateTime sentAt;
}
