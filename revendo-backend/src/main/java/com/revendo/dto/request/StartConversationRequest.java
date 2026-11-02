package com.revendo.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Incoming payload for POST /api/messages/conversations.
 * The buyer sends this to contact the seller of a listing.
 * Requirements: 9.1, 9.2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartConversationRequest {

    @NotNull(message = "Listing ID is required.")
    private Long listingId;
}
