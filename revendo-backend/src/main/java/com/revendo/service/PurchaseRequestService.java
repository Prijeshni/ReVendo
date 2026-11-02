package com.revendo.service;

import com.revendo.dto.response.PurchaseRequestResponse;
import com.revendo.exception.ConflictException;
import com.revendo.exception.ResourceNotFoundException;
import com.revendo.exception.UnauthorizedException;
import com.revendo.exception.ValidationException;
import com.revendo.model.Listing;
import com.revendo.model.Listing.ListingStatus;
import com.revendo.model.ListingImage;
import com.revendo.model.PurchaseRequest;
import com.revendo.model.PurchaseRequest.RequestStatus;
import com.revendo.model.PurchaseRequest.RequestType;
import com.revendo.model.User;
import com.revendo.repository.ListingRepository;
import com.revendo.repository.PurchaseRequestRepository;
import com.revendo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseRequestService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final ListingRepository         listingRepository;
    private final UserRepository            userRepository;

    // ── Create a purchase request (FOR_SALE) ────────────────────────

    @Transactional
    public PurchaseRequestResponse createRequest(Long listingId, Long buyerId) {
        return doCreateRequest(listingId, buyerId, RequestType.PURCHASE, null);
    }

    // ── Create a rental request (FOR_RENT) ──────────────────────────

    @Transactional
    public PurchaseRequestResponse createRentalRequest(Long listingId, Long buyerId, int duration) {
        if (duration < 1 || duration > 90) {
            throw new ValidationException("Rental duration must be between 1 and 90.");
        }
        return doCreateRequest(listingId, buyerId, RequestType.RENTAL, duration);
    }

    // ── Shared create logic ─────────────────────────────────────────

    private PurchaseRequestResponse doCreateRequest(
            Long listingId, Long buyerId, RequestType type, Integer duration) {

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));

        if (listing.getStatus() != ListingStatus.Active) {
            throw new ConflictException("This listing is no longer available.");
        }

        // Validate listing type matches request type
        if (type == RequestType.PURCHASE
                && listing.getListingType() != Listing.ListingType.FOR_SALE) {
            throw new ValidationException("This listing is for rent, not for sale.");
        }
        if (type == RequestType.RENTAL
                && listing.getListingType() != Listing.ListingType.FOR_RENT) {
            throw new ValidationException("This listing is for sale, not for rent.");
        }

        if (listing.getSeller().getId().equals(buyerId)) {
            throw new UnauthorizedException("You cannot request your own listing.");
        }

        // Prevent duplicate pending request
        purchaseRequestRepository
                .findByListingIdAndBuyerIdAndStatus(listingId, buyerId, RequestStatus.PENDING)
                .ifPresent(existing -> {
                    throw new ConflictException("You already have a pending request for this listing.");
                });

        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", buyerId));

        PurchaseRequest req = PurchaseRequest.builder()
                .listing(listing)
                .buyer(buyer)
                .seller(listing.getSeller())
                .priceAtRequest(listing.getPrice())
                .requestType(type)
                .rentalDuration(duration)
                .status(RequestStatus.PENDING)
                .build();

        return toResponse(purchaseRequestRepository.save(req));
    }

    // ── Seller accepts a request ────────────────────────────────────

    @Transactional
    public PurchaseRequestResponse acceptRequest(Long requestId, Long sellerId) {
        PurchaseRequest req = loadRequest(requestId);
        assertSeller(req, sellerId);
        assertPending(req);

        req.setStatus(RequestStatus.ACCEPTED);
        req.setResponseDate(LocalDateTime.now());
        purchaseRequestRepository.save(req);

        // Mark listing as Sold (purchase) or Rented (rental)
        Listing listing = req.getListing();
        if (req.getRequestType() == RequestType.RENTAL) {
            listing.setStatus(ListingStatus.Rented);
        } else {
            listing.setStatus(ListingStatus.Sold);
        }
        listingRepository.save(listing);

        // Reject any other pending requests for the same listing
        List<PurchaseRequest> others = purchaseRequestRepository
                .findByListingIdAndStatus(listing.getId(), RequestStatus.PENDING);
        for (PurchaseRequest other : others) {
            other.setStatus(RequestStatus.REJECTED);
            other.setResponseDate(LocalDateTime.now());
        }
        purchaseRequestRepository.saveAll(others);

        return toResponse(req);
    }

    // ── Seller rejects a request ────────────────────────────────────

    @Transactional
    public PurchaseRequestResponse rejectRequest(Long requestId, Long sellerId) {
        PurchaseRequest req = loadRequest(requestId);
        assertSeller(req, sellerId);
        assertPending(req);

        req.setStatus(RequestStatus.REJECTED);
        req.setResponseDate(LocalDateTime.now());
        return toResponse(purchaseRequestRepository.save(req));
    }

    // ── Read methods ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PurchaseRequestResponse> getRequestsForSeller(Long sellerId) {
        return purchaseRequestRepository.findBySellerIdOrderByRequestDateDesc(sellerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PurchaseRequestResponse> getRequestsForBuyer(Long buyerId) {
        return purchaseRequestRepository.findByBuyerIdOrderByRequestDateDesc(buyerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Helpers ────────────────────────────────────────────────────

    private PurchaseRequest loadRequest(Long id) {
        return purchaseRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseRequest", id));
    }

    private void assertSeller(PurchaseRequest req, Long sellerId) {
        if (!req.getSeller().getId().equals(sellerId)) {
            throw new UnauthorizedException("Only the listing seller can respond to this request.");
        }
    }

    private void assertPending(PurchaseRequest req) {
        if (req.getStatus() != RequestStatus.PENDING) {
            throw new ConflictException("This request has already been " + req.getStatus().toString().toLowerCase() + ".");
        }
    }

    private PurchaseRequestResponse toResponse(PurchaseRequest r) {
        String buyerName = r.getBuyer().getFirstName() + " " + r.getBuyer().getLastName();
        String sellerName = r.getSeller().getFirstName() + " " + r.getSeller().getLastName();
        String imageUrl = r.getListing().getImages().stream()
                .findFirst().map(ListingImage::getImageUrl).orElse(null);

        return PurchaseRequestResponse.builder()
                .requestId(r.getId())
                .listingId(r.getListing().getId())
                .listingTitle(r.getListing().getTitle())
                .listingImageUrl(imageUrl)
                .buyerId(r.getBuyer().getId())
                .buyerName(buyerName)
                .sellerId(r.getSeller().getId())
                .sellerName(sellerName)
                .price(r.getPriceAtRequest())
                .requestType(r.getRequestType())
                .rentalDuration(r.getRentalDuration())
                .status(r.getStatus())
                .requestDate(r.getRequestDate())
                .responseDate(r.getResponseDate())
                .build();
    }
}
