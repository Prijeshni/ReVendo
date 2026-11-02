package com.revendo.dto.response;

import com.revendo.model.Listing;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Outgoing payload for a single row in the admin listing list.
 * Requirements: 13.7, 13.9
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminListingResponse {

    private Long                 listingId;
    private String               title;
    private String               sellerName;
    private Long                 sellerId;
    private String               category;
    private Listing.ListingType  listingType;
    private Listing.ListingStatus status;
    private BigDecimal           price;
    private LocalDateTime        createdAt;
}
