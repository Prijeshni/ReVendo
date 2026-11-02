package com.revendo.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Incoming payload for PUT /api/negotiations/:offerId/counter.
 * Requirements: 8.7, 8.11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CounterOfferRequest {

    @NotNull(message = "Counter amount is required.")
    @DecimalMin(value = "0.01", message = "Counter amount must be at least 0.01.")
    private BigDecimal counterAmount;
}
