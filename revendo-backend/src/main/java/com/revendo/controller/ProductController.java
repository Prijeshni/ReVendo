package com.revendo.controller;

import com.revendo.dto.request.CreateListingRequest;
import com.revendo.dto.request.RentListingRequest;
import com.revendo.dto.request.UpdateListingRequest;
import com.revendo.dto.response.ListingResponse;
import com.revendo.dto.response.TransactionResponse;
import com.revendo.exception.ValidationException;
import com.revendo.model.Listing;
import com.revendo.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * REST controller for product (listing) endpoints.
 *
 * Public  (no JWT):
 *   GET  /api/products          — paginated browse with filters
 *   GET  /api/products/:id      — single listing details
 *
 * Authenticated (JWT required):
 *   POST   /api/products         — create listing  (multipart/form-data)
 *   PUT    /api/products/:id     — update listing  (multipart/form-data, owner only)
 *   DELETE /api/products/:id     — soft-delete     (owner only)
 *
 * Requirements: 4.1, 5.1, 5.3, 6.1, 6.2, 12.1
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private static final List<String> ALLOWED_SORT_VALUES =
            List.of("PRICE_ASC", "PRICE_DESC", "NEWEST");

    private final ProductService productService;

    // ----------------------------------------------------------------
    // GET /api/products  — public paginated listing browse
    // ----------------------------------------------------------------

    /**
     * Returns a paginated list of active listings with optional filters.
     *
     * Query params:
     *   page        (int,     default 0)
     *   size        (int,     default 12, capped at 12 in service)
     *   keyword     (String,  optional)
     *   category    (String,  optional — enum name)
     *   condition   (String,  optional — enum name)
     *   listingType (String,  optional — FOR_SALE | FOR_RENT)
     *   minPrice    (decimal, optional)
     *   maxPrice    (decimal, optional)
     *   sort        (String,  optional — PRICE_ASC | PRICE_DESC | NEWEST)
     */
    @GetMapping
    public ResponseEntity<Page<ListingResponse>> getListings(
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "12") int    size,
            @RequestParam(required = false)    String keyword,
            @RequestParam(required = false)    String category,
            @RequestParam(required = false)    String condition,
            @RequestParam(required = false)    String listingType,
            @RequestParam(required = false)    BigDecimal minPrice,
            @RequestParam(required = false)    BigDecimal maxPrice,
            @RequestParam(required = false)    String sort) {

        // Validate price range at controller level (Requirement 4.8)
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new ValidationException(
                    "Minimum price must not exceed maximum price.");
        }

        Sort sortSpec = resolveSort(sort);
        PageRequest pageable = PageRequest.of(page, Math.min(size, 12), sortSpec);

        Page<ListingResponse> result = productService.getListings(
                keyword,
                parseEnum(Listing.Category.class,    category),
                parseEnum(Listing.Condition.class,   condition),
                parseEnum(Listing.ListingType.class, listingType),
                minPrice, maxPrice,
                pageable);

        return ResponseEntity.ok(result);
    }

    // ----------------------------------------------------------------
    // GET /api/products/:id  — public single listing
    // ----------------------------------------------------------------

    @GetMapping("/{id}")
    public ResponseEntity<ListingResponse> getListingById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getListingById(id));
    }

    // ----------------------------------------------------------------
    // POST /api/products  — authenticated: create listing
    // ----------------------------------------------------------------

    /**
     * Creates a new listing for the authenticated seller.
     * Accepts multipart/form-data: JSON fields + up to 5 image files.
     * Returns 201 Created with the saved listing.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ListingResponse> createListing(
            @Valid @ModelAttribute CreateListingRequest dto,
            @RequestParam("images") List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails principal) {

        validateImages(images);
        Long sellerId = extractUserId(principal);
        ListingResponse saved = productService.createListing(dto, sellerId, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ----------------------------------------------------------------
    // PUT /api/products/:id  — authenticated: update listing (owner)
    // ----------------------------------------------------------------

    /**
     * Updates fields on an existing listing owned by the authenticated user.
     * Images param is optional — omit to keep existing images.
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ListingResponse> updateListing(
            @PathVariable Long id,
            @Valid @ModelAttribute UpdateListingRequest dto,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails principal) {

        if (images != null && !images.isEmpty()) {
            validateImages(images);
        }
        Long requesterId = extractUserId(principal);
        ListingResponse updated = productService.updateListing(id, dto, requesterId, images);
        return ResponseEntity.ok(updated);
    }

    // ----------------------------------------------------------------
    // DELETE /api/products/:id  — authenticated: soft-delete (owner)
    // ----------------------------------------------------------------

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteListing(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {

        Long requesterId = extractUserId(principal);
        productService.deleteListing(id, requesterId);
        return ResponseEntity.ok(Map.of("message", "Listing deleted successfully."));
    }

    // ----------------------------------------------------------------
    // POST /api/products/:id/purchase  — authenticated: buy now
    // ----------------------------------------------------------------

    /**
     * Processes a purchase for the authenticated buyer.
     * Returns 200 with transaction confirmation.
     * Requirements: 7.2
     */
    @PostMapping("/{id}/purchase")
    public ResponseEntity<TransactionResponse> purchaseListing(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {

        Long buyerId = extractUserId(principal);
        TransactionResponse tx = productService.purchaseListing(id, buyerId);
        return ResponseEntity.ok(tx);
    }

    // ----------------------------------------------------------------
    // POST /api/products/:id/rent  — authenticated: rent now
    // ----------------------------------------------------------------

    /**
     * Processes a rental for the authenticated buyer.
     * Body: { "duration": int (1–90) }
     * Returns 200 with transaction confirmation.
     * Requirements: 7.4
     */
    @PostMapping("/{id}/rent")
    public ResponseEntity<TransactionResponse> rentListing(
            @PathVariable Long id,
            @Valid @RequestBody RentListingRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        Long buyerId = extractUserId(principal);
        TransactionResponse tx = productService.rentListing(
                id, buyerId, request.getDuration());
        return ResponseEntity.ok(tx);
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    /** Extracts the numeric userId from the JWT principal (username is the ID string). */
    private Long extractUserId(UserDetails principal) {
        return Long.parseLong(principal.getUsername());
    }

    /**
     * Validates image count (1–5) and file types (jpg/jpeg/png).
     * Requirement 5.5, 5.8, 5.9, 5.11
     */
    private void validateImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            throw new ValidationException("At least one image is required.");
        }
        if (images.size() > 5) {
            throw new ValidationException(
                    "Maximum of 5 images allowed per listing.");
        }
        for (MultipartFile img : images) {
            String name = img.getOriginalFilename();
            if (name == null) {
                throw new ValidationException("Invalid image file.");
            }
            String lower = name.toLowerCase();
            if (!lower.endsWith(".jpg") && !lower.endsWith(".jpeg")
                    && !lower.endsWith(".png")) {
                throw new ValidationException(
                        "Only JPG, JPEG, and PNG image files are accepted.");
            }
            if (img.getSize() > 5 * 1024 * 1024) {
                throw new ValidationException(
                        "Each image must not exceed 5 MB.");
            }
        }
    }

    /**
     * Maps the sort query parameter to a Spring {@link Sort}.
     * Defaults to newest first when unrecognised or absent.
     */
    private Sort resolveSort(String sort) {
        if (sort == null) return Sort.by(Sort.Direction.DESC, "createdAt");
        return switch (sort.toUpperCase()) {
            case "PRICE_ASC"  -> Sort.by(Sort.Direction.ASC,  "price");
            case "PRICE_DESC" -> Sort.by(Sort.Direction.DESC, "price");
            default           -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    /** Parses a nullable String into an enum constant; returns null if input is null/blank. */
    private <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(
                    "Invalid value '" + value + "' for " + type.getSimpleName() + ". "
                    + "Allowed values: " + Arrays.toString(type.getEnumConstants()));
        }
    }
}
