package com.revendo.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Incoming payload for POST /api/products/:id/rent.
 *
 * Requirements: 7.3, 7.4
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentListingRequest {

    /**
     * Duration in units of the listing's rentalPeriodUnit.
     * Must be a whole number between 1 and 90 (Requirement 7.3).
     */
    @NotNull(message = "Rental duration is required.")
    @Min(value = 1,  message = "Rental duration must be at least 1.")
    @Max(value = 90, message = "Rental duration must not exceed 90.")
    private Integer duration;
}
