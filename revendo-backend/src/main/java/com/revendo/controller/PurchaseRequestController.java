package com.revendo.controller;

import com.revendo.dto.response.PurchaseRequestResponse;
import com.revendo.service.PurchaseRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Purchase request workflow controller.
 *
 * POST /api/purchase-requests              — buyer creates a request
 * GET  /api/purchase-requests/seller      — seller's received requests
 * GET  /api/purchase-requests/buyer       — buyer's sent requests
 * PUT  /api/purchase-requests/:id/accept  — seller accepts
 * PUT  /api/purchase-requests/:id/reject  — seller rejects
 */
@RestController
@RequestMapping("/api/purchase-requests")
@RequiredArgsConstructor
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;

    @PostMapping
    public ResponseEntity<PurchaseRequestResponse> createRequest(
            @RequestParam Long listingId,
            @AuthenticationPrincipal UserDetails principal) {
        Long buyerId = Long.parseLong(principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(purchaseRequestService.createRequest(listingId, buyerId));
    }

    /**
     * Buyer creates a rental request with a duration.
     * Body: { "duration": int }
     */
    @PostMapping("/rental")
    public ResponseEntity<PurchaseRequestResponse> createRentalRequest(
            @RequestParam Long listingId,
            @RequestBody java.util.Map<String, Object> body,
            @AuthenticationPrincipal UserDetails principal) {
        Long buyerId = Long.parseLong(principal.getUsername());
        Object durObj = body.get("duration");
        if (durObj == null) {
            throw new com.revendo.exception.ValidationException("Rental duration is required.");
        }
        int duration = ((Number) durObj).intValue();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(purchaseRequestService.createRentalRequest(listingId, buyerId, duration));
    }

    @GetMapping("/seller")
    public ResponseEntity<List<PurchaseRequestResponse>> getSellerRequests(
            @AuthenticationPrincipal UserDetails principal) {
        Long sellerId = Long.parseLong(principal.getUsername());
        return ResponseEntity.ok(purchaseRequestService.getRequestsForSeller(sellerId));
    }

    @GetMapping("/buyer")
    public ResponseEntity<List<PurchaseRequestResponse>> getBuyerRequests(
            @AuthenticationPrincipal UserDetails principal) {
        Long buyerId = Long.parseLong(principal.getUsername());
        return ResponseEntity.ok(purchaseRequestService.getRequestsForBuyer(buyerId));
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<PurchaseRequestResponse> acceptRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Long sellerId = Long.parseLong(principal.getUsername());
        return ResponseEntity.ok(purchaseRequestService.acceptRequest(id, sellerId));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<PurchaseRequestResponse> rejectRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Long sellerId = Long.parseLong(principal.getUsername());
        return ResponseEntity.ok(purchaseRequestService.rejectRequest(id, sellerId));
    }
}
