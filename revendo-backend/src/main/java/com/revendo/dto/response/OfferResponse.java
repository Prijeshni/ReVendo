package com.revendo.dto.response;

import com.revendo.model.Offer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Outgoing payload for a single offer/negotiation.
 * Requirements: 8.2, 8.5–8.11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferResponse {

    private Long             offerId;
    private Long             listingId;
    private String           listingTitle;
    private Long             buyerId;
    private String           buyerDisplayName;
    private BigDecimal       offerAmount;
    private BigDecimal       counterAmount;
    private Offer.OfferStatus status;
    private int              roundCount;
    private LocalDateTime    createdAt;
    private LocalDateTime    updatedAt;
}
