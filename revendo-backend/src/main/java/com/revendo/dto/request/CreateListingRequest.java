package com.revendo.dto.request;

import com.revendo.model.Listing;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Incoming payload for creating a new listing (POST /api/products).
 * Images are received as MultipartFile[] separately in the controller (Task 5.8).
 *
 * Requirements: 5.3, 5.4, 5.5
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateListingRequest {

    @NotBlank(message = "Title is required.")
    @Size(max = 150, message = "Title must not exceed 150 characters.")
    private String title;

    @NotBlank(message = "Description is required.")
    @Size(max = 2000, message = "Description must not exceed 2000 characters.")
    private String description;

    @NotNull(message = "Category is required.")
    private Listing.Category category;

    @NotNull(message = "Condition is required.")
    private Listing.Condition condition;

    @NotNull(message = "Listing type is required.")
    private Listing.ListingType listingType;

    @NotNull(message = "Price is required.")
    @DecimalMin(value = "0.01", message = "Price must be at least 0.01.")
    @DecimalMax(value = "999999999.99", message = "Price exceeds the maximum allowed value.")
    private BigDecimal price;

    /** Required when listingType is FOR_RENT. */
    private Listing.RentalPeriodUnit rentalPeriodUnit;
}
