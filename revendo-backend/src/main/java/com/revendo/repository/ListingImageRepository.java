package com.revendo.repository;

import com.revendo.model.ListingImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link ListingImage} entities.
 */
@Repository
public interface ListingImageRepository extends JpaRepository<ListingImage, Long> {

    /**
     * Returns all images for a given listing, ordered by display position.
     */
    List<ListingImage> findByListingIdOrderByDisplayOrderAsc(Long listingId);

    /**
     * Deletes all images belonging to a specific listing.
     * Used when replacing images during a listing update.
     */
    void deleteByListingId(Long listingId);
}
