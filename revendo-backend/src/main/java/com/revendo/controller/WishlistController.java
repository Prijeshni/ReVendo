package com.revendo.controller;

import com.revendo.dto.response.WishlistItemResponse;
import com.revendo.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for wishlist endpoints.
 *
 * All endpoints require JWT authentication.
 *
 * GET    /api/wishlist                — get all saved listings
 * POST   /api/wishlist/:listingId     — add listing to wishlist
 * DELETE /api/wishlist/:listingId     — remove listing from wishlist
 * GET    /api/wishlist/:listingId/status — check if listing is wishlisted
 *
 * Requirements: 10.1–10.8
 */
@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    // ----------------------------------------------------------------
    // GET /api/wishlist  — get all saved listings
    // ----------------------------------------------------------------

    /**
     * Returns all wishlist entries for the authenticated user.
     * Includes sold/rented listings so the frontend can show "No Longer Available".
     */
    @GetMapping
    public ResponseEntity<List<WishlistItemResponse>> getWishlist(
            @AuthenticationPrincipal UserDetails principal) {

        return ResponseEntity.ok(wishlistService.getWishlist(extractUserId(principal)));
    }

    // ----------------------------------------------------------------
    // POST /api/wishlist/:listingId  — add to wishlist
    // ----------------------------------------------------------------

    /**
     * Adds a listing to the authenticated user's wishlist.
     * Idempotent — returns 200 with the entry whether newly created or already existing.
     * Returns 201 on first save, 200 on subsequent calls (idempotent).
     */
    @PostMapping("/{listingId}")
    public ResponseEntity<WishlistItemResponse> addToWishlist(
            @PathVariable Long listingId,
            @AuthenticationPrincipal UserDetails principal) {

        Long userId = extractUserId(principal);
        boolean alreadyExists = wishlistService.isWishlisted(userId, listingId);
        WishlistItemResponse response = wishlistService.addToWishlist(userId, listingId);

        return alreadyExists
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(201).body(response);
    }

    // ----------------------------------------------------------------
    // DELETE /api/wishlist/:listingId  — remove from wishlist
    // ----------------------------------------------------------------

    /**
     * Removes a listing from the authenticated user's wishlist.
     * Returns 200 regardless of whether the entry existed (safe DELETE).
     */
    @DeleteMapping("/{listingId}")
    public ResponseEntity<Map<String, String>> removeFromWishlist(
            @PathVariable Long listingId,
            @AuthenticationPrincipal UserDetails principal) {

        wishlistService.removeFromWishlist(extractUserId(principal), listingId);
        return ResponseEntity.ok(
                Map.of("message", "Listing removed from wishlist."));
    }

    // ----------------------------------------------------------------
    // GET /api/wishlist/:listingId/status  — check wishlisted status
    // ----------------------------------------------------------------

    /**
     * Returns whether the given listing is in the authenticated user's wishlist.
     * Response: { "wishlisted": true/false }
     */
    @GetMapping("/{listingId}/status")
    public ResponseEntity<Map<String, Boolean>> isWishlisted(
            @PathVariable Long listingId,
            @AuthenticationPrincipal UserDetails principal) {

        boolean result = wishlistService.isWishlisted(
                extractUserId(principal), listingId);
        return ResponseEntity.ok(Map.of("wishlisted", result));
    }

    // ----------------------------------------------------------------
    // Private helper
    // ----------------------------------------------------------------

    private Long extractUserId(UserDetails principal) {
        return Long.parseLong(principal.getUsername());
    }
}
