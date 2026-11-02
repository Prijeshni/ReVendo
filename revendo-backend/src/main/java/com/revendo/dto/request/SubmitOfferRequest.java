package com.revendo.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Incoming payload for POST /api/negotiations.
 * Requirements: 8.1, 8.2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitOfferRequest {

    @NotNull(message = "Listing ID is required.")
    private Long listingId;

    @NotNull(message = "Offer amount is required.")
    @DecimalMin(value = "0.01", message = "Offer amount must be at least 0.01.")
    private BigDecimal offerAmount;
}
