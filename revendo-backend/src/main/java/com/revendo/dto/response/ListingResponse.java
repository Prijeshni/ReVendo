package com.revendo.dto.response;

import com.revendo.model.Listing;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Outgoing payload for a single listing.
 * Used by ProductService and later by ProductController (Task 5.8).
 *
 * Requirements: 4.1, 5.1, 6.1, 6.2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingResponse {

    private Long   id;
    private String title;
    private String description;

    /** Human-readable category string (e.g. "Home Appliances"). */
    private String category;

    /** Human-readable condition string (e.g. "Like New"). */
    private String condition;

    private Listing.ListingType      listingType;
    private BigDecimal               price;
    private Listing.RentalPeriodUnit rentalPeriodUnit;
    private Listing.ListingStatus    status;

    /** Relative or absolute URLs for each image, in display order. */
    private List<String> imageUrls;

    private String        sellerDisplayName;
    private LocalDateTime sellerJoinDate;
    private LocalDateTime createdAt;
}
