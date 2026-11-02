package com.revendo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Outgoing payload for a single conversation summary (list view).
 *
 * Requirements: 9.1, 9.2, 9.5
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationResponse {

    private Long   conversationId;
    private Long   listingId;
    private String listingTitle;

    private Long   buyerId;
    private String buyerDisplayName;

    private Long   sellerId;
    private String sellerDisplayName;

    /**
     * Last message text truncated to 100 characters for the list view.
     * Null if no messages have been sent yet (Requirement 9.5).
     */
    private String lastMessagePreview;

    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;
}
