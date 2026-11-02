package com.revendo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a buyer's request to purchase or rent a listing.
 * The seller must accept or reject the request before the listing status changes.
 *
 * Status flow: PENDING → ACCEPTED (listing becomes Sold/Rented) or REJECTED (listing stays Active)
 */
@Entity
@Table(name = "purchase_requests",
    indexes = {
        @Index(name = "idx_pr_listing_id", columnList = "listing_id"),
        @Index(name = "idx_pr_buyer_id",   columnList = "buyer_id"),
        @Index(name = "idx_pr_seller_id",  columnList = "seller_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseRequest {

    public enum RequestStatus {
        PENDING, ACCEPTED, REJECTED
    }

    public enum RequestType {
        PURCHASE, RENTAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @NotNull
    @Column(name = "price_at_request", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAtRequest;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false,
            columnDefinition = "ENUM('PURCHASE','RENTAL') DEFAULT 'PURCHASE'")
    @Builder.Default
    private RequestType requestType = RequestType.PURCHASE;

    /** Rental duration (1–90). Only set for RENTAL requests. */
    @Column(name = "rental_duration")
    private Integer rentalDuration;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false,
            columnDefinition = "ENUM('PENDING','ACCEPTED','REJECTED') DEFAULT 'PENDING'")
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "request_date", nullable = false, updatable = false)
    private LocalDateTime requestDate;

    @Column(name = "response_date")
    private LocalDateTime responseDate;

    @PrePersist
    protected void onCreate() {
        if (requestDate == null) requestDate = LocalDateTime.now();
    }
}
