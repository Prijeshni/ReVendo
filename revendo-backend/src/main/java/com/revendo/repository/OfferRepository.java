package com.revendo.repository;

import com.revendo.model.Offer;
import com.revendo.model.Offer.OfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Offer} entities.
 */
@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {

    /**
     * Find all offers on a specific listing (seller view).
     */
    List<Offer> findByListingId(Long listingId);

    /**
     * Find all offers submitted by a specific buyer.
     */
    List<Offer> findByBuyerId(Long buyerId);

    /**
     * Find an active (Pending or Countered) offer for a given buyer-listing pair.
     * Used to enforce the duplicate offer prevention rule (Requirement 8.12).
     */
    Optional<Offer> findByListingIdAndBuyerIdAndStatusIn(
            Long listingId, Long buyerId, List<OfferStatus> statuses);

    /**
     * Find all offers on a listing that are Pending or Countered.
     */
    List<Offer> findByListingIdAndStatusIn(Long listingId, List<OfferStatus> statuses);
}
