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
 * Outgoing payload for a single wishlist entry.
 * Carries enough listing detail to render a product card on the Wishlist page.
 *
 * Requirements: 10.1, 10.5, 10.6, 10.7
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistItemResponse {

    private Long   wishlistItemId;
    private Long   listingId;
    private String title;
    private String category;
    private String condition;

    private Listing.ListingType   listingType;
    private Listing.ListingStatus listingStatus;

    private BigDecimal   price;
    private List<String> imageUrls;

    private String        sellerDisplayName;
    private LocalDateTime addedAt;
}
