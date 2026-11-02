package com.revendo.service;

import com.revendo.dto.response.OfferResponse;
import com.revendo.exception.ConflictException;
import com.revendo.exception.ResourceNotFoundException;
import com.revendo.exception.UnauthorizedException;
import com.revendo.exception.ValidationException;
import com.revendo.model.Listing;
import com.revendo.model.Notification;
import com.revendo.model.Offer;
import com.revendo.model.Offer.OfferStatus;
import com.revendo.model.User;
import com.revendo.repository.ListingRepository;
import com.revendo.repository.NotificationRepository;
import com.revendo.repository.OfferRepository;
import com.revendo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for price negotiation.
 *
 * Task 7.1 methods:
 *   submitOffer  — buyer submits an initial offer on an active listing
 *   acceptOffer  — seller (Pending) or buyer (Countered) accepts
 *   rejectOffer  — seller (Pending) or buyer (Countered) rejects
 *   counterOffer — seller counters a Pending offer (max 5 rounds)
 *
 * Requirements: 8.2, 8.3, 8.5–8.12
 */
@Service
@RequiredArgsConstructor
public class NegotiationService {

    private static final int MAX_ROUNDS = 5;

    private static final List<OfferStatus> ACTIVE_STATUSES =
            List.of(OfferStatus.Pending, OfferStatus.Countered);

    private final OfferRepository        offerRepository;
    private final ListingRepository      listingRepository;
    private final UserRepository         userRepository;
    private final NotificationRepository notificationRepository;

    // ----------------------------------------------------------------
    // submitOffer  (Requirement 8.1, 8.2, 8.3, 8.12)
    // ----------------------------------------------------------------

    /**
     * Buyer submits an initial offer on a listing.
     *
     * Rules:
     * - Listing must exist and be Active.
     * - Buyer must not be the seller.
     * - No existing Pending/Countered offer from this buyer on this listing.
     *
     * @param listingId   the target listing
     * @param buyerId     the authenticated buyer's ID (from JWT)
     * @param offerAmount the offered price (> 0)
     * @return the created offer as {@link OfferResponse}
     */
    @Transactional
    public OfferResponse submitOffer(Long listingId, Long buyerId, BigDecimal offerAmount) {

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));

        // Listing must be Active
        if (listing.getStatus() != Listing.ListingStatus.Active) {
            throw new ValidationException(
                    "Offers can only be submitted on active listings.");
        }

        // Buyer must not be the seller
        if (listing.getSeller().getId().equals(buyerId)) {
            throw new UnauthorizedException(
                    "You cannot submit an offer on your own listing.");
        }

        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", buyerId));

        // Duplicate offer prevention (Requirement 8.12)
        offerRepository.findByListingIdAndBuyerIdAndStatusIn(
                listingId, buyerId, ACTIVE_STATUSES)
                .ifPresent(existing -> {
                    throw new ConflictException(
                            "You already have a pending or countered offer on this listing.");
                });

        Offer offer = Offer.builder()
                .listing(listing)
                .buyer(buyer)
                .offerAmount(offerAmount)
                .status(OfferStatus.Pending)
                .roundCount(0)
                .build();

        Offer saved = offerRepository.save(offer);

        // Notify seller (Requirement 8.3)
        createNotification(listing.getSeller(),
                buyer.getFirstName() + " " + buyer.getLastName()
                + " made an offer of " + offerAmount + " on your listing \""
                + listing.getTitle() + "\".");

        return toResponse(saved);
    }

    // ----------------------------------------------------------------
    // acceptOffer  (Requirement 8.5, 8.9)
    // ----------------------------------------------------------------

    /**
     * Accepts an offer.
     * - Seller accepts a Pending offer.
     * - Buyer accepts a Countered offer.
     *
     * @param offerId the offer to accept
     * @param actorId the authenticated user's ID (from JWT)
     * @return the updated offer as {@link OfferResponse}
     */
    @Transactional
    public OfferResponse acceptOffer(Long offerId, Long actorId) {

        Offer offer = loadOffer(offerId);
        assertOfferIsActive(offer);

        Long sellerId = offer.getListing().getSeller().getId();
        Long buyerId  = offer.getBuyer().getId();

        if (offer.getStatus() == OfferStatus.Pending) {
            // Only the seller may accept a Pending offer
            assertIsSeller(actorId, sellerId);
        } else {
            // Countered — only the buyer may accept
            assertIsBuyer(actorId, buyerId);
        }

        offer.setStatus(OfferStatus.Accepted);
        Offer saved = offerRepository.save(offer);

        // Notify the other party
        notifyOtherParty(offer, actorId, "accepted your offer on listing \""
                + offer.getListing().getTitle() + "\".");

        return toResponse(saved);
    }

    // ----------------------------------------------------------------
    // rejectOffer  (Requirement 8.6, 8.10)
    // ----------------------------------------------------------------

    /**
     * Rejects an offer.
     * - Seller rejects a Pending offer.
     * - Buyer rejects a Countered offer.
     *
     * @param offerId the offer to reject
     * @param actorId the authenticated user's ID (from JWT)
     * @return the updated offer as {@link OfferResponse}
     */
    @Transactional
    public OfferResponse rejectOffer(Long offerId, Long actorId) {

        Offer offer = loadOffer(offerId);
        assertOfferIsActive(offer);

        Long sellerId = offer.getListing().getSeller().getId();
        Long buyerId  = offer.getBuyer().getId();

        if (offer.getStatus() == OfferStatus.Pending) {
            assertIsSeller(actorId, sellerId);
        } else {
            assertIsBuyer(actorId, buyerId);
        }

        offer.setStatus(OfferStatus.Rejected);
        Offer saved = offerRepository.save(offer);

        notifyOtherParty(offer, actorId, "rejected your offer on listing \""
                + offer.getListing().getTitle() + "\".");

        return toResponse(saved);
    }

    // ----------------------------------------------------------------
    // counterOffer  (Requirement 8.7, 8.11)
    // ----------------------------------------------------------------

    /**
     * Seller submits a counter offer. Max 5 rounds enforced.
     *
     * @param offerId       the offer to counter
     * @param sellerId      the authenticated seller's ID (from JWT)
     * @param counterAmount the seller's counter price
     * @return the updated offer as {@link OfferResponse}
     */
    @Transactional
    public OfferResponse counterOffer(Long offerId, Long sellerId, BigDecimal counterAmount) {

        Offer offer = loadOffer(offerId);
        assertOfferIsActive(offer);

        // Only the seller can counter (Requirement 8.7)
        assertIsSeller(sellerId, offer.getListing().getSeller().getId());

        // Only Pending offers can be countered by the seller
        if (offer.getStatus() != OfferStatus.Pending) {
            throw new ValidationException(
                    "Only pending offers can be countered.");
        }

        // Round count enforcement (Requirement 8.11)
        if (offer.getRoundCount() >= MAX_ROUNDS) {
            throw new ConflictException(
                    "Maximum negotiation rounds reached. Please accept or reject.");
        }

        offer.setCounterAmount(counterAmount);
        offer.setStatus(OfferStatus.Countered);
        offer.setRoundCount(offer.getRoundCount() + 1);
        Offer saved = offerRepository.save(offer);

        // Notify buyer
        User seller = offer.getListing().getSeller();
        createNotification(offer.getBuyer(),
                seller.getFirstName() + " " + seller.getLastName()
                + " countered your offer with " + counterAmount
                + " on listing \"" + offer.getListing().getTitle() + "\".");

        return toResponse(saved);
    }

    // ----------------------------------------------------------------
    // Read methods (used by NegotiationController)
    // ----------------------------------------------------------------

    /**
     * Returns all offers on a listing — seller view.
     * Caller should verify actor is the listing's seller.
     */
    @Transactional(readOnly = true)
    public List<OfferResponse> getOffersForListing(Long listingId, Long requesterId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));

        if (!listing.getSeller().getId().equals(requesterId)) {
            throw new UnauthorizedException(
                    "Only the listing owner can view its offers.");
        }

        return offerRepository.findByListingId(listingId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns all offers submitted by the authenticated buyer.
     */
    @Transactional(readOnly = true)
    public List<OfferResponse> getMyOffers(Long buyerId) {
        return offerRepository.findByBuyerId(buyerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    private Offer loadOffer(Long offerId) {
        return offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer", offerId));
    }

    private void assertOfferIsActive(Offer offer) {
        if (offer.getStatus() == OfferStatus.Accepted
                || offer.getStatus() == OfferStatus.Rejected) {
            throw new ConflictException(
                    "This offer has already been finalised (status: "
                    + offer.getStatus() + ").");
        }
    }

    private void assertIsSeller(Long actorId, Long sellerId) {
        if (!actorId.equals(sellerId)) {
            throw new UnauthorizedException(
                    "Only the listing seller can perform this action.");
        }
    }

    private void assertIsBuyer(Long actorId, Long buyerId) {
        if (!actorId.equals(buyerId)) {
            throw new UnauthorizedException(
                    "Only the offer buyer can perform this action.");
        }
    }

    /**
     * Notifies the party that did NOT perform the action.
     */
    private void notifyOtherParty(Offer offer, Long actorId, String verb) {
        Long sellerId = offer.getListing().getSeller().getId();
        Long buyerId  = offer.getBuyer().getId();

        User recipient = actorId.equals(sellerId)
                ? offer.getBuyer()
                : offer.getListing().getSeller();

        User actor = actorId.equals(sellerId)
                ? offer.getListing().getSeller()
                : offer.getBuyer();

        createNotification(recipient,
                actor.getFirstName() + " " + actor.getLastName() + " " + verb);
    }

    private void createNotification(User recipient, String message) {
        notificationRepository.save(
                Notification.builder()
                        .user(recipient)
                        .message(message.length() > 500
                                ? message.substring(0, 497) + "..."
                                : message)
                        .build());
    }

    /**
     * Maps an {@link Offer} entity to an {@link OfferResponse} DTO.
     */
    private OfferResponse toResponse(Offer o) {
        String buyerName = o.getBuyer().getFirstName()
                + " " + o.getBuyer().getLastName();
        return OfferResponse.builder()
                .offerId(o.getId())
                .listingId(o.getListing().getId())
                .listingTitle(o.getListing().getTitle())
                .buyerId(o.getBuyer().getId())
                .buyerDisplayName(buyerName)
                .offerAmount(o.getOfferAmount())
                .counterAmount(o.getCounterAmount())
                .status(o.getStatus())
                .roundCount(o.getRoundCount())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}
