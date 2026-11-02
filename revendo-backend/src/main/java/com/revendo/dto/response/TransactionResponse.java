package com.revendo.dto.response;

import com.revendo.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Outgoing payload confirming a completed purchase or rental.
 *
 * Requirements: 7.2, 7.4
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private Long                       transactionId;
    private Long                       listingId;
    private String                     listingTitle;
    private Transaction.TransactionType transactionType;
    private BigDecimal                 pricePaid;
    private LocalDateTime              transactionDate;

    /** Rentals only. */
    private LocalDate                    rentalStartDate;
    private LocalDate                    rentalEndDate;
    private Integer                      rentalDuration;
    private Transaction.RentalPeriodUnit rentalPeriodUnit;
}
