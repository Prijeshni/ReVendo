package com.revendo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity representing the `offers` table.
 *
 * The uniqueness constraint "(listing_id, buyer_id) where status IN (Pending, Countered)"
 * is enforced at the service layer rather than by a partial DB index, because MySQL
 * does not support partial indexes natively.
 */
@Entity
@Table(
    name = "offers",
    indexes = {
        @Index(name = "idx_offers_listing_id", columnList = "listing_id"),
        @Index(name = "idx_offers_buyer_id",   columnList = "buyer_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Offer {

    // ----- Enums -----

    public enum OfferStatus {
        Pending, Accepted, Rejected, Countered
    }

    // ----- Columns -----

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
    @DecimalMin("0.01")
    @Column(name = "offer_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal offerAmount;

    /** Populated by the seller when countering. */
    @DecimalMin("0.01")
    @Column(name = "counter_amount", precision = 12, scale = 2)
    private BigDecimal counterAmount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false,
            columnDefinition = "ENUM('Pending','Accepted','Rejected','Countered') DEFAULT 'Pending'")
    @Builder.Default
    private OfferStatus status = OfferStatus.Pending;

    /** Number of completed negotiation rounds (max 5). */
    @Column(name = "round_count", nullable = false)
    @Builder.Default
    private int roundCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ----- Lifecycle -----

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
