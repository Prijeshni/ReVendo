package com.revendo.repository;

import com.revendo.model.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link WishlistItem} entities.
 */
@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    /**
     * Find a specific wishlist entry for a user-listing pair.
     */
    Optional<WishlistItem> findByUserIdAndListingId(Long userId, Long listingId);

    /**
     * All wishlist items for a given user.
     */
    List<WishlistItem> findByUserId(Long userId);

    /**
     * Check whether a listing is already in a user's wishlist (idempotence check).
     */
    boolean existsByUserIdAndListingId(Long userId, Long listingId);

    /**
     * Remove a specific wishlist entry.
     */
    void deleteByUserIdAndListingId(Long userId, Long listingId);
}
