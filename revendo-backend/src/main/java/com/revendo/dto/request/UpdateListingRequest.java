package com.revendo.dto.request;

import com.revendo.model.Listing;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Incoming payload for updating an existing listing (PUT /api/products/:id).
 *
 * All fields are optional — only non-null fields are applied.
 * Requirements: 12.3, 12.4, 12.5
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateListingRequest {

    @Size(max = 150, message = "Title must not exceed 150 characters.")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters.")
    private String description;

    private Listing.Category category;
    private Listing.Condition condition;
    private Listing.ListingType listingType;

    @DecimalMin(value = "0.01", message = "Price must be at least 0.01.")
    @DecimalMax(value = "999999999.99", message = "Price exceeds the maximum allowed value.")
    private BigDecimal price;

    private Listing.RentalPeriodUnit rentalPeriodUnit;
}
