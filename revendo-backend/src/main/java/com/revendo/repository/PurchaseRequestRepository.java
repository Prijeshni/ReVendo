package com.revendo.repository;

import com.revendo.model.PurchaseRequest;
import com.revendo.model.PurchaseRequest.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {

    /** All requests received by a seller (for the seller dashboard). */
    List<PurchaseRequest> findBySellerIdOrderByRequestDateDesc(Long sellerId);

    /** All requests made by a buyer (for the buyer dashboard). */
    List<PurchaseRequest> findByBuyerIdOrderByRequestDateDesc(Long buyerId);

    /** Pending requests for a specific seller (notification badge). */
    List<PurchaseRequest> findBySellerIdAndStatus(Long sellerId, RequestStatus status);

    /** All pending requests for a specific listing (used when accepting one to reject the rest). */
    List<PurchaseRequest> findByListingIdAndStatus(Long listingId, RequestStatus status);

    /** Check for duplicate active request (prevent multiple pending requests for same listing+buyer). */
    Optional<PurchaseRequest> findByListingIdAndBuyerIdAndStatus(Long listingId, Long buyerId, RequestStatus status);

    /** Check if any pending request exists for a listing. */
    boolean existsByListingIdAndStatus(Long listingId, RequestStatus status);
}
