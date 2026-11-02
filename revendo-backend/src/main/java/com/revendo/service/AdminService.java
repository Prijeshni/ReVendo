package com.revendo.service;

import com.revendo.dto.response.AdminListingResponse;
import com.revendo.dto.response.AdminSummaryResponse;
import com.revendo.dto.response.AdminUserResponse;
import com.revendo.exception.ResourceNotFoundException;
import com.revendo.exception.ValidationException;
import com.revendo.model.Listing;
import com.revendo.model.Listing.ListingStatus;
import com.revendo.model.Offer;
import com.revendo.model.User;
import com.revendo.repository.ListingRepository;
import com.revendo.repository.OfferRepository;
import com.revendo.repository.TransactionRepository;
import com.revendo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for the Admin Dashboard.
 *
 * Task 10.2 methods:
 *   getSummary       — platform statistics for the summary card
 *   getUsers         — paginated user list with optional search
 *   suspendUser      — set user + all their Active listings to Suspended
 *   reactivateUser   — restore user + their Suspended listings to Active
 *   getListings      — paginated listing list with optional filters
 *   removeListing    — soft-remove a listing; cancel pending offers on it
 *
 * Requirements: 13.1–13.9
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private static final int ADMIN_PAGE_SIZE = 50;

    private final UserRepository        userRepository;
    private final ListingRepository     listingRepository;
    private final TransactionRepository transactionRepository;
    private final OfferRepository       offerRepository;

    // ----------------------------------------------------------------
    // getSummary  (Requirement 13.3)
    // ----------------------------------------------------------------

    /**
     * Returns platform-wide statistics for the admin summary card.
     *
     * "Pending review" = listings in Suspended status that need admin attention
     * (set there by suspendUser cascade and awaiting admin review/reactivation).
     * "Completed transactions" = all recorded purchases and rentals.
     *
     * Uses dedicated count queries rather than paginated fetches for efficiency.
     */
    @Transactional(readOnly = true)
    public AdminSummaryResponse getSummary() {
        long totalUsers                 = userRepository.count();
        long totalActiveListings        = listingRepository.countByStatus(ListingStatus.Active);
        long totalCompletedTransactions = transactionRepository.count();
        long totalPendingReview         = listingRepository.countByStatus(ListingStatus.Suspended);

        return AdminSummaryResponse.builder()
                .totalUsers(totalUsers)
                .totalActiveListings(totalActiveListings)
                .totalCompletedTransactions(totalCompletedTransactions)
                .totalListingsPendingReview(totalPendingReview)
                .build();
    }

    // ----------------------------------------------------------------
    // getUsers  (Requirements 13.4, 13.9)
    // ----------------------------------------------------------------

    /**
     * Returns a paginated list of all users, optionally filtered by a
     * case-insensitive partial match on name or email.
     *
     * @param page        zero-based page index
     * @param searchQuery partial name/email (null or blank = no filter)
     */
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getUsers(int page, String searchQuery) {
        Pageable pageable = PageRequest.of(page, ADMIN_PAGE_SIZE);
        String q = (searchQuery != null && !searchQuery.isBlank()) ? searchQuery.trim() : "";

        Page<User> users = userRepository.findBySearchQuery(q, pageable);
        List<AdminUserResponse> content = users.getContent()
                .stream()
                .map(this::toAdminUserResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, users.getTotalElements());
    }

    // ----------------------------------------------------------------
    // suspendUser  (Requirement 13.5)
    // ----------------------------------------------------------------

    /**
     * Suspends a user account and cascades the suspension to all their
     * Active listings.
     *
     * @param userId the user to suspend
     * @throws ResourceNotFoundException if the user does not exist
     * @throws ValidationException       if the user is already suspended
     */
    @Transactional
    public AdminUserResponse suspendUser(Long userId) {
        User user = loadUser(userId);

        if (user.getStatus() == User.Status.Suspended) {
            throw new ValidationException("User is already suspended.");
        }

        user.setStatus(User.Status.Suspended);
        userRepository.save(user);

        // Cascade: set all Active listings to Suspended (Requirement 13.5)
        List<Listing> activeListings =
                listingRepository.findBySellerIdAndStatus(userId, ListingStatus.Active);
        activeListings.forEach(l -> l.setStatus(ListingStatus.Suspended));
        listingRepository.saveAll(activeListings);

        return toAdminUserResponse(user);
    }

    // ----------------------------------------------------------------
    // reactivateUser  (Requirement 13.6)
    // ----------------------------------------------------------------

    /**
     * Reactivates a suspended user and restores their Suspended listings
     * to Active.
     *
     * @param userId the user to reactivate
     * @throws ResourceNotFoundException if the user does not exist
     * @throws ValidationException       if the user is not suspended
     */
    @Transactional
    public AdminUserResponse reactivateUser(Long userId) {
        User user = loadUser(userId);

        if (user.getStatus() != User.Status.Suspended) {
            throw new ValidationException("User is not suspended.");
        }

        user.setStatus(User.Status.Active);
        userRepository.save(user);

        // Cascade: restore Suspended → Active for this user's listings (Requirement 13.6)
        List<Listing> suspendedListings =
                listingRepository.findBySellerIdAndStatus(userId, ListingStatus.Suspended);
        suspendedListings.forEach(l -> l.setStatus(ListingStatus.Active));
        listingRepository.saveAll(suspendedListings);

        return toAdminUserResponse(user);
    }

    // ----------------------------------------------------------------
    // getListings  (Requirements 13.7, 13.9)
    // ----------------------------------------------------------------

    /**
     * Returns a paginated list of all listings with optional filters.
     *
     * @param page        zero-based page index
     * @param category    filter by category (null = all)
     * @param status      filter by listing status (null = all)
     * @param listingType filter by FOR_SALE / FOR_RENT (null = all)
     */
    @Transactional(readOnly = true)
    public Page<AdminListingResponse> getListings(
            int page,
            Listing.Category category,
            ListingStatus status,
            Listing.ListingType listingType) {

        Pageable pageable = PageRequest.of(page, ADMIN_PAGE_SIZE);
        Page<Listing> listings =
                listingRepository.findByAdminFilters(category, status, listingType, pageable);

        List<AdminListingResponse> content = listings.getContent()
                .stream()
                .map(this::toAdminListingResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, listings.getTotalElements());
    }

    // ----------------------------------------------------------------
    // removeListing  (Requirement 13.8)
    // ----------------------------------------------------------------

    /**
     * Sets a listing's status to Removed (soft delete) and cancels any
     * Pending or Countered offers associated with it.
     *
     * Transactions (completed purchases/rentals) are historical records
     * and are NOT deleted.
     *
     * @param listingId the listing to remove
     * @throws ResourceNotFoundException if the listing does not exist
     * @throws ValidationException       if the listing is already removed/deleted
     */
    @Transactional
    public AdminListingResponse removeListing(Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));

        if (listing.getStatus() == ListingStatus.Removed
                || listing.getStatus() == ListingStatus.Deleted) {
            throw new ValidationException(
                    "Listing is already removed or deleted.");
        }

        listing.setStatus(ListingStatus.Removed);
        listingRepository.save(listing);

        // Cancel all Pending/Countered offers (Requirement 13.8)
        List<Offer> pendingOffers = offerRepository.findByListingIdAndStatusIn(
                listingId,
                List.of(Offer.OfferStatus.Pending, Offer.OfferStatus.Countered));
        pendingOffers.forEach(o -> o.setStatus(Offer.OfferStatus.Rejected));
        offerRepository.saveAll(pendingOffers);

        return toAdminListingResponse(listing);
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private AdminUserResponse toAdminUserResponse(User u) {
        return AdminUserResponse.builder()
                .userId(u.getId())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .email(u.getEmail())
                .status(u.getStatus())
                .joinDate(u.getCreatedAt())
                .build();
    }

    private AdminListingResponse toAdminListingResponse(Listing l) {
        String sellerName = l.getSeller().getFirstName()
                + " " + l.getSeller().getLastName();
        return AdminListingResponse.builder()
                .listingId(l.getId())
                .title(l.getTitle())
                .sellerName(sellerName)
                .sellerId(l.getSeller().getId())
                .category(l.getCategory() != null ? l.getCategory().toString() : null)
                .listingType(l.getListingType())
                .status(l.getStatus())
                .price(l.getPrice())
                .createdAt(l.getCreatedAt())
                .build();
    }
}
