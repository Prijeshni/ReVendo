package com.revendo.controller;

import com.revendo.dto.request.CounterOfferRequest;
import com.revendo.dto.request.SubmitOfferRequest;
import com.revendo.dto.response.OfferResponse;
import com.revendo.service.NegotiationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for price negotiation (offer) endpoints.
 *
 * All endpoints require JWT authentication.
 *
 * POST   /api/negotiations                          — submit offer (buyer)
 * GET    /api/negotiations/listing/:listingId       — offers for a listing (seller)
 * GET    /api/negotiations/my-offers                — buyer's own offers
 * PUT    /api/negotiations/:offerId/accept          — accept offer
 * PUT    /api/negotiations/:offerId/reject          — reject offer
 * PUT    /api/negotiations/:offerId/counter         — counter offer (seller only)
 *
 * Requirements: 8.4, 8.5, 8.6, 8.7
 */
@RestController
@RequestMapping("/api/negotiations")
@RequiredArgsConstructor
public class NegotiationController {

    private final NegotiationService negotiationService;

    // ----------------------------------------------------------------
    // POST /api/negotiations  — buyer submits an offer
    // ----------------------------------------------------------------

    /**
     * Returns 201 Created with the new offer.
     * Body: { "listingId": Long, "offerAmount": BigDecimal }
     */
    @PostMapping
    public ResponseEntity<OfferResponse> submitOffer(
            @Valid @RequestBody SubmitOfferRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        Long buyerId = extractUserId(principal);
        OfferResponse offer = negotiationService.submitOffer(
                request.getListingId(), buyerId, request.getOfferAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(offer);
    }

    // ----------------------------------------------------------------
    // GET /api/negotiations/listing/:listingId  — seller view
    // ----------------------------------------------------------------

    /**
     * Returns all offers on a listing. Only the listing's seller may call this.
     */
    @GetMapping("/listing/{listingId}")
    public ResponseEntity<List<OfferResponse>> getOffersForListing(
            @PathVariable Long listingId,
            @AuthenticationPrincipal UserDetails principal) {

        Long requesterId = extractUserId(principal);
        return ResponseEntity.ok(
                negotiationService.getOffersForListing(listingId, requesterId));
    }

    // ----------------------------------------------------------------
    // GET /api/negotiations/my-offers  — buyer's own offers
    // ----------------------------------------------------------------

    @GetMapping("/my-offers")
    public ResponseEntity<List<OfferResponse>> getMyOffers(
            @AuthenticationPrincipal UserDetails principal) {

        Long buyerId = extractUserId(principal);
        return ResponseEntity.ok(negotiationService.getMyOffers(buyerId));
    }

    // ----------------------------------------------------------------
    // PUT /api/negotiations/:offerId/accept
    // ----------------------------------------------------------------

    /**
     * Accept a Pending offer (seller) or a Countered offer (buyer).
     * Returns 200 with the updated offer.
     */
    @PutMapping("/{offerId}/accept")
    public ResponseEntity<OfferResponse> acceptOffer(
            @PathVariable Long offerId,
            @AuthenticationPrincipal UserDetails principal) {

        Long actorId = extractUserId(principal);
        return ResponseEntity.ok(negotiationService.acceptOffer(offerId, actorId));
    }

    // ----------------------------------------------------------------
    // PUT /api/negotiations/:offerId/reject
    // ----------------------------------------------------------------

    /**
     * Reject a Pending offer (seller) or a Countered offer (buyer).
     * Returns 200 with the updated offer.
     */
    @PutMapping("/{offerId}/reject")
    public ResponseEntity<OfferResponse> rejectOffer(
            @PathVariable Long offerId,
            @AuthenticationPrincipal UserDetails principal) {

        Long actorId = extractUserId(principal);
        return ResponseEntity.ok(negotiationService.rejectOffer(offerId, actorId));
    }

    // ----------------------------------------------------------------
    // PUT /api/negotiations/:offerId/counter
    // ----------------------------------------------------------------

    /**
     * Seller counters a Pending offer.
     * Body: { "counterAmount": BigDecimal }
     * Returns 200 with the updated offer.
     */
    @PutMapping("/{offerId}/counter")
    public ResponseEntity<OfferResponse> counterOffer(
            @PathVariable Long offerId,
            @Valid @RequestBody CounterOfferRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        Long sellerId = extractUserId(principal);
        return ResponseEntity.ok(
                negotiationService.counterOffer(offerId, sellerId, request.getCounterAmount()));
    }

    // ----------------------------------------------------------------
    // Private helper
    // ----------------------------------------------------------------

    private Long extractUserId(UserDetails principal) {
        return Long.parseLong(principal.getUsername());
    }
}
