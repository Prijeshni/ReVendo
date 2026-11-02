package com.revendo.model;

import com.revendo.model.converter.CategoryConverter;
import com.revendo.model.converter.ConditionConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity representing the `listings` table.
 *
 * Note: the DB column for condition is named `condition_type` to avoid
 * collision with the SQL reserved word `CONDITION`.
 */
@Entity
@Table(
    name = "listings",
    indexes = {
        @Index(name = "idx_listings_seller_id",  columnList = "seller_id"),
        @Index(name = "idx_listings_created_at", columnList = "created_at"),
        @Index(name = "idx_listings_status",     columnList = "status")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Listing {

    // ----- Enums -----

    public enum Category {
        Electronics, Furniture, Books,
        @SuppressWarnings("java:S115")   // allow spaces in display name via toString
        Home_Appliances, Household_Goods;

        /** Returns the human-readable form used on the frontend / DB. */
        @Override
        public String toString() {
            return name().replace('_', ' ');
        }
    }

    public enum Condition {
        New,
        @SuppressWarnings("java:S115")
        Like_New,
        Good, Fair, Poor;

        @Override
        public String toString() {
            return name().replace('_', ' ');
        }
    }

    public enum ListingType {
        FOR_SALE, FOR_RENT
    }

    public enum RentalPeriodUnit {
        PER_DAY, PER_WEEK, PER_MONTH
    }

    public enum ListingStatus {
        Active, Sold, Rented, Deleted, Removed, Suspended
    }

    // ----- Columns -----

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @NotBlank
    @Size(max = 150)
    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @NotBlank
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Convert(converter = CategoryConverter.class)
    @Column(name = "category", nullable = false,
            columnDefinition = "ENUM('Electronics','Furniture','Books','Home Appliances','Household Goods')")
    private Category category;

    @NotNull
    @Convert(converter = ConditionConverter.class)
    @Column(name = "condition_type", nullable = false,
            columnDefinition = "ENUM('New','Like New','Good','Fair','Poor')")
    private Condition condition;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false,
            columnDefinition = "ENUM('FOR_SALE','FOR_RENT')")
    private ListingType listingType;

    @NotNull
    @DecimalMin(value = "0.01")
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "rental_period_unit",
            columnDefinition = "ENUM('PER_DAY','PER_WEEK','PER_MONTH')")
    private RentalPeriodUnit rentalPeriodUnit;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false,
            columnDefinition = "ENUM('Active','Sold','Rented','Deleted','Removed','Suspended') DEFAULT 'Active'")
    @Builder.Default
    private ListingStatus status = ListingStatus.Active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Optimistic locking version — prevents concurrent purchase/rental conflicts.
     * Hibernate increments this on every UPDATE; a stale-read throws
     * {@link jakarta.persistence.OptimisticLockException} automatically.
     */
    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    // ----- Images (cascade) -----

    @OneToMany(
        mappedBy = "listing",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<ListingImage> images = new ArrayList<>();

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
