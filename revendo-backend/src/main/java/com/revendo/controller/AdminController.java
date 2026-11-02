package com.revendo.controller;

import com.revendo.dto.response.AdminListingResponse;
import com.revendo.dto.response.AdminSummaryResponse;
import com.revendo.dto.response.AdminUserResponse;
import com.revendo.exception.ValidationException;
import com.revendo.model.Listing;
import com.revendo.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for admin dashboard endpoints.
 *
 * All routes are under /api/admin/** which SecurityConfig locks to ROLE_ADMIN.
 * No @PreAuthorize annotations are needed — the filter chain handles it.
 *
 * GET  /api/admin/summary                  — platform statistics
 * GET  /api/admin/users                    — paginated user list (50/page)
 * PUT  /api/admin/users/:id/suspend        — suspend a user
 * PUT  /api/admin/users/:id/reactivate     — reactivate a user
 * GET  /api/admin/listings                 — paginated listing list (50/page)
 * PUT  /api/admin/listings/:id/remove      — remove a listing
 *
 * Requirements: 13.1–13.9
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // GET /api/admin/summary
    @GetMapping("/summary")
    public ResponseEntity<AdminSummaryResponse> getSummary() {
        return ResponseEntity.ok(adminService.getSummary());
    }

    // GET /api/admin/users?page=0&search=
    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserResponse>> getUsers(
            @RequestParam(defaultValue = "0") int    page,
            @RequestParam(required = false)   String search) {
        return ResponseEntity.ok(adminService.getUsers(page, search));
    }

    // PUT /api/admin/users/:id/suspend
    @PutMapping("/users/{id}/suspend")
    public ResponseEntity<AdminUserResponse> suspendUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.suspendUser(id));
    }

    // PUT /api/admin/users/:id/reactivate
    @PutMapping("/users/{id}/reactivate")
    public ResponseEntity<AdminUserResponse> reactivateUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.reactivateUser(id));
    }

    // GET /api/admin/listings?page=0&category=&status=&listingType=
    @GetMapping("/listings")
    public ResponseEntity<Page<AdminListingResponse>> getListings(
            @RequestParam(defaultValue = "0") int    page,
            @RequestParam(required = false)   String category,
            @RequestParam(required = false)   String status,
            @RequestParam(required = false)   String listingType) {

        return ResponseEntity.ok(adminService.getListings(
                page,
                parseEnum(Listing.Category.class,     category),
                parseEnum(Listing.ListingStatus.class, status),
                parseEnum(Listing.ListingType.class,  listingType)));
    }

    // PUT /api/admin/listings/:id/remove
    @PutMapping("/listings/{id}/remove")
    public ResponseEntity<AdminListingResponse> removeListing(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.removeListing(id));
    }

    // ----------------------------------------------------------------
    // Private helper
    // ----------------------------------------------------------------

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(
                    "Invalid value '" + value + "' for " + type.getSimpleName() + ".");
        }
    }
}
