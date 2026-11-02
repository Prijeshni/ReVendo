package com.revendo.service;

import com.revendo.dto.response.WishlistItemResponse;
import com.revendo.exception.ResourceNotFoundException;
import com.revendo.model.Listing;
import com.revendo.model.ListingImage;
import com.revendo.model.User;
import com.revendo.model.WishlistItem;
import com.revendo.repository.ListingRepository;
import com.revendo.repository.UserRepository;
import com.revendo.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for wishlist management.
 *
 * Task 9.1 methods:
 *   addToWishlist    — idempotent: returns existing entry without creating a duplicate
 *   removeFromWishlist — silently no-ops if entry does not exist
 *   getWishlist      — all saved listings for the user with full product detail
 *   isWishlisted     — boolean status check for a single listing
 *
 * Requirements: 10.1–10.3, 10.5, 10.6, 10.8
 */
@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final ListingRepository      listingRepository;
    private final UserRepository         userRepository;

    // ----------------------------------------------------------------
    // addToWishlist  (Requirements 10.1, 10.2, 10.8)
    // ----------------------------------------------------------------

    /**
     * Adds a listing to the authenticated user's wishlist.
     *
     * Idempotent: if the listing is already wishlisted, the existing entry is
     * returned without creating a duplicate (Requirement 10.8).
     *
     * @param userId    the authenticated user's ID (from JWT)
     * @param listingId the listing to save
     * @return the wishlist entry (new or existing)
     * @throws ResourceNotFoundException if the user or listing does not exist
     */
    @Transactional
    public WishlistItemResponse addToWishlist(Long userId, Long listingId) {

        // Idempotence check — return existing entry without saving again
        return wishlistItemRepository
                .findByUserIdAndListingId(userId, listingId)
                .map(this::toResponse)
                .orElseGet(() -> {

                    User user = userRepository.findById(userId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException("User", userId));

                    Listing listing = listingRepository.findById(listingId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException("Listing", listingId));

                    WishlistItem item = WishlistItem.builder()
                            .user(user)
                            .listing(listing)
                            .build();

                    return toResponse(wishlistItemRepository.save(item));
                });
    }

    // ----------------------------------------------------------------
    // removeFromWishlist  (Requirement 10.3)
    // ----------------------------------------------------------------

    /**
     * Removes a listing from the authenticated user's wishlist.
     * Silently no-ops if the entry does not exist (safe DELETE).
     *
     * @param userId    the authenticated user's ID (from JWT)
     * @param listingId the listing to remove
     */
    @Transactional
    public void removeFromWishlist(Long userId, Long listingId) {
        wishlistItemRepository.deleteByUserIdAndListingId(userId, listingId);
    }

    // ----------------------------------------------------------------
    // getWishlist  (Requirements 10.5, 10.6, 10.7)
    // ----------------------------------------------------------------

    /**
     * Returns all saved listings for the authenticated user.
     * Sold and Rented listings are included with their status so the frontend
     * can display a "No Longer Available" badge (Requirement 10.7).
     *
     * @param userId the authenticated user's ID (from JWT)
     * @return list of wishlist entries with product detail
     */
    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getWishlist(Long userId) {
        return wishlistItemRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    // isWishlisted  (Requirement 10.2)
    // ----------------------------------------------------------------

    /**
     * Returns {@code true} if the listing is currently in the user's wishlist.
     *
     * @param userId    the authenticated user's ID (from JWT)
     * @param listingId the listing to check
     * @return boolean status
     */
    @Transactional(readOnly = true)
    public boolean isWishlisted(Long userId, Long listingId) {
        return wishlistItemRepository.existsByUserIdAndListingId(userId, listingId);
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    private WishlistItemResponse toResponse(WishlistItem item) {
        Listing l = item.getListing();

        List<String> imageUrls = l.getImages().stream()
                .map(ListingImage::getImageUrl)
                .collect(Collectors.toList());

        String sellerDisplay = l.getSeller().getFirstName()
                + " " + l.getSeller().getLastName();

        return WishlistItemResponse.builder()
                .wishlistItemId(item.getId())
                .listingId(l.getId())
                .title(l.getTitle())
                .category(l.getCategory() != null ? l.getCategory().toString() : null)
                .condition(l.getCondition() != null ? l.getCondition().toString() : null)
                .listingType(l.getListingType())
                .listingStatus(l.getStatus())
                .price(l.getPrice())
                .imageUrls(imageUrls)
                .sellerDisplayName(sellerDisplay)
                .addedAt(item.getAddedAt())
                .build();
    }
}
