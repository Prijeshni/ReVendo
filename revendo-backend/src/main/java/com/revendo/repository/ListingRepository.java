package com.revendo.repository;

import com.revendo.model.Listing;
import com.revendo.model.Listing.Category;
import com.revendo.model.Listing.Condition;
import com.revendo.model.Listing.ListingStatus;
import com.revendo.model.Listing.ListingType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository for {@link Listing} entities.
 *
 * The primary search query uses optional (nullable) parameters so that
 * any combination of filters can be passed without building a separate
 * Criteria API query. A null parameter disables that filter clause.
 */
@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {

    /**
     * Dynamic search across all active listings.
     *
     * Every parameter is optional — pass {@code null} to skip that filter.
     *
     * @param keyword     substring to match against title OR description (case-insensitive)
     * @param category    exact category to match
     * @param condition   exact condition to match
     * @param listingType FOR_SALE or FOR_RENT
     * @param minPrice    lower bound (inclusive)
     * @param maxPrice    upper bound (inclusive)
     * @param pageable    sort and pagination
     * @return a page of matching active listings
     */
    @Query("""
           SELECT l FROM Listing l
           WHERE l.status = 'Active'
             AND (:keyword   IS NULL OR LOWER(l.title)       LIKE LOWER(CONCAT('%', :keyword, '%'))
                                     OR LOWER(l.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
             AND (:category    IS NULL OR l.category    = :category)
             AND (:condition   IS NULL OR l.condition   = :condition)
             AND (:listingType IS NULL OR l.listingType = :listingType)
             AND (:minPrice    IS NULL OR l.price >= :minPrice)
             AND (:maxPrice    IS NULL OR l.price <= :maxPrice)
           """)
    Page<Listing> findByFilters(
            @Param("keyword")     String keyword,
            @Param("category")    Category category,
            @Param("condition")   Condition condition,
            @Param("listingType") ListingType listingType,
            @Param("minPrice")    BigDecimal minPrice,
            @Param("maxPrice")    BigDecimal maxPrice,
            Pageable pageable
    );

    /**
     * Retrieve all listings created by a specific seller.
     */
    List<Listing> findBySellerId(Long sellerId);

    /**
     * All listings for a seller with a specific status.
     * Used by suspendUser (Active→Suspended) and reactivateUser (Suspended→Active).
     */
    List<Listing> findBySellerIdAndStatus(Long sellerId, Listing.ListingStatus status);

    /**
     * Admin paginated listing search — all statuses, optional filters.
     * Null parameters are ignored (no filter applied).
     */
    @Query("""
           SELECT l FROM Listing l
           WHERE (:category    IS NULL OR l.category    = :category)
             AND (:status      IS NULL OR l.status      = :status)
             AND (:listingType IS NULL OR l.listingType = :listingType)
           """)
    Page<Listing> findByAdminFilters(
            @Param("category")    Listing.Category    category,
            @Param("status")      Listing.ListingStatus status,
            @Param("listingType") Listing.ListingType listingType,
            Pageable pageable
    );

    /**
     * Check whether a seller owns a specific listing.
     */
    boolean existsByIdAndSellerId(Long listingId, Long sellerId);

    /**
     * Count listings with a given status — used by the admin summary.
     * More efficient than a paginated query for count-only operations.
     */
    long countByStatus(Listing.ListingStatus status);
}
