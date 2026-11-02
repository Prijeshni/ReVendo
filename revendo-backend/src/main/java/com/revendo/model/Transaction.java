package com.revendo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA entity representing the `transactions` table.
 * One transaction per listing (UNIQUE constraint on listing_id).
 */
@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    // ----- Enums -----

    public enum TransactionType {
        PURCHASE, RENTAL
    }

    public enum RentalPeriodUnit {
        PER_DAY, PER_WEEK, PER_MONTH
    }

    // ----- Columns -----

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false, unique = true)
    private Listing listing;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false,
            columnDefinition = "ENUM('PURCHASE','RENTAL')")
    private TransactionType transactionType;

    @NotNull
    @Column(name = "price_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePaid;

    @Column(name = "transaction_date", nullable = false, updatable = false)
    private LocalDateTime transactionDate;

    /** Rentals only — the date the rental period begins. */
    @Column(name = "rental_start_date")
    private LocalDate rentalStartDate;

    /** Rentals only — calculated end date. */
    @Column(name = "rental_end_date")
    private LocalDate rentalEndDate;

    /** Rentals only — number of units (1–90). */
    @Column(name = "rental_duration")
    private Integer rentalDuration;

    /** Rentals only — PER_DAY / PER_WEEK / PER_MONTH. */
    @Enumerated(EnumType.STRING)
    @Column(name = "rental_period_unit",
            columnDefinition = "ENUM('PER_DAY','PER_WEEK','PER_MONTH')")
    private RentalPeriodUnit rentalPeriodUnit;

    // ----- Lifecycle -----

    @PrePersist
    protected void onCreate() {
        if (transactionDate == null) {
            transactionDate = LocalDateTime.now();
        }
    }
}
