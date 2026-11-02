package com.revendo.service;

import com.revendo.dto.request.CreateListingRequest;
import com.revendo.dto.request.UpdateListingRequest;
import com.revendo.dto.response.ListingResponse;
import com.revendo.dto.response.TransactionResponse;
import com.revendo.exception.ConflictException;
import com.revendo.exception.ResourceNotFoundException;
import com.revendo.exception.UnauthorizedException;
import com.revendo.exception.ValidationException;
import com.revendo.model.Listing;
import com.revendo.model.Listing.ListingStatus;
import com.revendo.model.ListingImage;
import com.revendo.model.Transaction;
import com.revendo.model.User;
import com.revendo.repository.ListingRepository;
import com.revendo.repository.ListingImageRepository;
import com.revendo.repository.TransactionRepository;
import com.revendo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for product (listing) operations.
 *
 * Task 5.1 methods:
 *   createListing  — authenticated seller creates a new listing with images
 *   getListingById — public single-listing fetch; 404 on deleted/removed
 *   getListings    — public paginated browse with optional filters
 *
 * Requirements: 4.2–4.7, 4.9–4.12, 5.6
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ListingRepository      listingRepository;
    private final UserRepository         userRepository;
    private final TransactionRepository  transactionRepository;
    private final ListingImageRepository listingImageRepository;

    @Value("${app.upload.dir:${user.dir}/src/main/resources/static/images}")
    private String uploadDir;

    /**
     * Ensures the upload directory exists at application startup.
     * Logs the resolved absolute path so it's visible in the console on launch.
     */
    @PostConstruct
    public void initUploadDirectory() {
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
            log.info("Image upload directory ready: {}", dir);
        } catch (IOException e) {
            log.error("Could NOT create image upload directory {}: {}", dir, e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // createListing
    // ----------------------------------------------------------------

    /**
     * Creates a new listing for the authenticated seller.
     *
     * @param dto      validated listing fields
     * @param sellerId the authenticated user's ID (from JWT)
     * @param images   1–5 image files (validated at controller layer in 5.8)
     * @return the persisted listing as a response DTO
     * @throws ResourceNotFoundException if the seller ID is not found
     * @throws ValidationException       if FOR_RENT listing is missing rentalPeriodUnit,
     *                                   or no images are provided
     */
    @Transactional
    public ListingResponse createListing(
            CreateListingRequest dto,
            Long sellerId,
            List<MultipartFile> images) {

        // Load seller
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", sellerId));

        // Business rule: FOR_RENT requires a rentalPeriodUnit
        if (dto.getListingType() == Listing.ListingType.FOR_RENT
                && dto.getRentalPeriodUnit() == null) {
            throw new ValidationException(
                    "Rental period unit is required for FOR_RENT listings.");
        }

        // Business rule: at least one image required
        if (images == null || images.isEmpty()) {
            throw new ValidationException("At least one image is required.");
        }

        // Persist images to disk and build ListingImage objects
        List<ListingImage> imageEntities = saveImages(images);

        // Build and save the Listing
        Listing listing = Listing.builder()
                .seller(seller)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .category(dto.getCategory())
                .condition(dto.getCondition())
                .listingType(dto.getListingType())
                .price(dto.getPrice())
                .rentalPeriodUnit(dto.getRentalPeriodUnit())
                .status(ListingStatus.Active)
                .build();

        // Wire images to the listing (bidirectional)
        for (ListingImage img : imageEntities) {
            img.setListing(listing);
        }
        listing.setImages(imageEntities);

        Listing saved = listingRepository.save(listing);
        return toResponse(saved);
    }

    // ----------------------------------------------------------------
    // getListingById
    // ----------------------------------------------------------------

    /**
     * Returns a single listing by ID.
     * Listings with status Deleted or Removed are treated as not found (Requirement 6.10).
     *
     * @param id the listing's database ID
     * @return the listing as a response DTO
     * @throws ResourceNotFoundException if not found or soft-deleted
     */
    @Transactional(readOnly = true)
    public ListingResponse getListingById(Long id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", id));

        if (listing.getStatus() == ListingStatus.Deleted
                || listing.getStatus() == ListingStatus.Removed) {
            throw new ResourceNotFoundException("Listing", id);
        }

        return toResponse(listing);
    }

    // ----------------------------------------------------------------
    // getListings  (public paginated browse)
    // ----------------------------------------------------------------

    /**
     * Returns a page of active listings with optional filters.
     * All filter parameters are nullable — pass null to skip that filter.
     * Page size is capped at 12 (Requirement 4.2).
     *
     * @param keyword     case-insensitive substring search on title/description
     * @param category    filter by category enum value
     * @param condition   filter by condition enum value
     * @param listingType FOR_SALE or FOR_RENT
     * @param minPrice    inclusive lower price bound
     * @param maxPrice    inclusive upper price bound
     * @param pageable    page/size/sort — size is capped at 12 internally
     * @return page of {@link ListingResponse}
     */
    @Transactional(readOnly = true)
    public Page<ListingResponse> getListings(
            String keyword,
            Listing.Category category,
            Listing.Condition condition,
            Listing.ListingType listingType,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {

        // Cap page size at 12 (Requirement 4.2)
        Pageable capped = capPageSize(pageable, 12);

        // Blank keyword treated as no keyword filter
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        Page<Listing> page = listingRepository.findByFilters(
                kw, category, condition, listingType, minPrice, maxPrice, capped);

        List<ListingResponse> content = page.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new PageImpl<>(content, capped, page.getTotalElements());
    }

    // ----------------------------------------------------------------
    // updateListing  (Requirement 12.2–12.5)
    // ----------------------------------------------------------------

    /**
     * Updates an existing listing. Only the listing's seller may do this.
     *
     * @param listingId   the listing to update
     * @param dto         new field values (null fields are left unchanged)
     * @param requesterId the authenticated user's ID (from JWT)
     * @param newImages   replacement images, or null/empty to keep existing ones
     * @return the updated listing as a response DTO
     * @throws ResourceNotFoundException if the listing does not exist
     * @throws UnauthorizedException     if the requester is not the owner
     * @throws ValidationException       if FOR_RENT and rentalPeriodUnit missing
     */
    @Transactional
    public ListingResponse updateListing(
            Long listingId,
            UpdateListingRequest dto,
            Long requesterId,
            List<MultipartFile> newImages) {

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));

        // Ownership check
        if (!listing.getSeller().getId().equals(requesterId)) {
            throw new UnauthorizedException(
                    "You are not authorised to edit this listing.");
        }

        // Apply field updates
        if (dto.getTitle()       != null) listing.setTitle(dto.getTitle());
        if (dto.getDescription() != null) listing.setDescription(dto.getDescription());
        if (dto.getCategory()    != null) listing.setCategory(dto.getCategory());
        if (dto.getCondition()   != null) listing.setCondition(dto.getCondition());
        if (dto.getListingType() != null) listing.setListingType(dto.getListingType());
        if (dto.getPrice()       != null) listing.setPrice(dto.getPrice());
        if (dto.getRentalPeriodUnit() != null) listing.setRentalPeriodUnit(dto.getRentalPeriodUnit());

        // Business rule: FOR_RENT must have rentalPeriodUnit after update
        if (listing.getListingType() == Listing.ListingType.FOR_RENT
                && listing.getRentalPeriodUnit() == null) {
            throw new ValidationException(
                    "Rental period unit is required for FOR_RENT listings.");
        }

        // Replace images if new ones supplied (Requirement 12.5)
        if (newImages != null && !newImages.isEmpty()) {
            listingImageRepository.deleteByListingId(listingId);
            listing.getImages().clear();

            List<ListingImage> replacements = saveImages(newImages);
            for (ListingImage img : replacements) {
                img.setListing(listing);
            }
            listing.setImages(replacements);
        }

        Listing saved = listingRepository.save(listing);
        return toResponse(saved);
    }

    // ----------------------------------------------------------------
    // deleteListing  (Requirement 12.6–12.7)
    // ----------------------------------------------------------------

    /**
     * Soft-deletes a listing by setting its status to {@code Deleted}.
     * Rejected if any Transaction exists for the listing (active or completed).
     *
     * @param listingId   the listing to delete
     * @param requesterId the authenticated user's ID (from JWT)
     * @throws ResourceNotFoundException if the listing does not exist
     * @throws UnauthorizedException     if the requester is not the owner
     * @throws ValidationException       if an active transaction exists
     */
    @Transactional
    public void deleteListing(Long listingId, Long requesterId) {

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));

        // Ownership check
        if (!listing.getSeller().getId().equals(requesterId)) {
            throw new UnauthorizedException(
                    "You are not authorised to delete this listing.");
        }

        // Block deletion if a transaction exists (Requirement 12.6)
        if (transactionRepository.existsByListingId(listingId)) {
            throw new ValidationException(
                    "Cannot delete a listing that has an existing transaction.");
        }

        listing.setStatus(ListingStatus.Deleted);
        listingRepository.save(listing);
    }

    // ----------------------------------------------------------------
    // purchaseListing  (Requirements 7.1–7.5, 7.7)
    // ----------------------------------------------------------------

    /**
     * Processes a "Buy Now" purchase for an authenticated buyer.
     *
     * Rules enforced inside a single @Transactional block:
     * - Listing must exist and be Active.
     * - Listing must be FOR_SALE.
     * - Buyer must not be the listing's seller (Requirement 7.7).
     * - No duplicate transaction (listing has UNIQUE constraint on transaction).
     * - Listing status is updated to Sold atomically (optimistic locking via @Version).
     *
     * @param listingId the listing to purchase
     * @param buyerId   the authenticated user's ID (from JWT — never trusted from client)
     * @return a {@link TransactionResponse} confirming the completed purchase
     */
    @Transactional
    public TransactionResponse purchaseListing(Long listingId, Long buyerId) {

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));

        // Must be Active
        if (listing.getStatus() != ListingStatus.Active) {
            throw new ConflictException(
                    "This listing is no longer available for purchase (status: "
                    + listing.getStatus() + ").");
        }

        // Must be FOR_SALE
        if (listing.getListingType() != Listing.ListingType.FOR_SALE) {
            throw new ValidationException(
                    "This listing is for rent, not for sale.");
        }

        // Buyer must not be the seller (Requirement 7.7)
        if (listing.getSeller().getId().equals(buyerId)) {
            throw new UnauthorizedException(
                    "You cannot purchase your own listing.");
        }

        // Load buyer
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", buyerId));

        // Update listing status to Sold (Hibernate @Version guards concurrent requests)
        listing.setStatus(ListingStatus.Sold);
        listingRepository.save(listing);

        // Record transaction
        Transaction tx = Transaction.builder()
                .listing(listing)
                .buyer(buyer)
                .transactionType(Transaction.TransactionType.PURCHASE)
                .pricePaid(listing.getPrice())
                .build();
        Transaction saved = transactionRepository.save(tx);

        return toTransactionResponse(saved);
    }

    // ----------------------------------------------------------------
    // rentListing  (Requirements 7.1, 7.3–7.5, 7.7)
    // ----------------------------------------------------------------

    /**
     * Processes a "Rent Now" rental for an authenticated buyer.
     *
     * Rules enforced:
     * - Listing must exist and be Active.
     * - Listing must be FOR_RENT.
     * - Buyer must not be the listing's seller.
     * - Duration must be 1–90 (validated at controller; double-checked here).
     * - Rental start = current UTC date; end = start + duration × unit.
     *
     * @param listingId the listing to rent
     * @param buyerId   the authenticated user's ID (from JWT)
     * @param duration  rental length in the listing's period unit (1–90)
     * @return a {@link TransactionResponse} confirming the rental
     */
    @Transactional
    public TransactionResponse rentListing(Long listingId, Long buyerId, int duration) {

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));

        // Must be Active
        if (listing.getStatus() != ListingStatus.Active) {
            throw new ConflictException(
                    "This listing is no longer available for rent (status: "
                    + listing.getStatus() + ").");
        }

        // Must be FOR_RENT
        if (listing.getListingType() != Listing.ListingType.FOR_RENT) {
            throw new ValidationException(
                    "This listing is for sale, not for rent.");
        }

        // Buyer must not be the seller (Requirement 7.7)
        if (listing.getSeller().getId().equals(buyerId)) {
            throw new UnauthorizedException(
                    "You cannot rent your own listing.");
        }

        // Duration range check (Requirement 7.3)
        if (duration < 1 || duration > 90) {
            throw new ValidationException(
                    "Rental duration must be between 1 and 90.");
        }

        // Load buyer
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", buyerId));

        // Calculate rental dates
        LocalDate startDate = LocalDate.now();
        LocalDate endDate   = calculateEndDate(startDate,
                duration, listing.getRentalPeriodUnit());

        // Map Listing.RentalPeriodUnit → Transaction.RentalPeriodUnit
        Transaction.RentalPeriodUnit txUnit =
                Transaction.RentalPeriodUnit.valueOf(
                        listing.getRentalPeriodUnit().name());

        // Update listing status to Rented
        listing.setStatus(ListingStatus.Rented);
        listingRepository.save(listing);

        // Record transaction
        Transaction tx = Transaction.builder()
                .listing(listing)
                .buyer(buyer)
                .transactionType(Transaction.TransactionType.RENTAL)
                .pricePaid(listing.getPrice())
                .rentalStartDate(startDate)
                .rentalEndDate(endDate)
                .rentalDuration(duration)
                .rentalPeriodUnit(txUnit)
                .build();
        Transaction saved = transactionRepository.save(tx);

        return toTransactionResponse(saved);
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    /**
     * Saves uploaded image files to the configured upload directory.
     *
     * Uses Files.copy() with REPLACE_EXISTING via InputStream instead of
     * file.transferTo() — transferTo() uses renameTo internally which fails
     * cross-device (temp dir → project dir) on Windows.
     *
     * The uploadDir is resolved to an absolute path so the JVM working directory
     * does not affect where files land.
     */
    private List<ListingImage> saveImages(List<MultipartFile> files) {
        List<ListingImage> result = new ArrayList<>();

        // Resolve to an absolute path — prevents cross-device rename failures
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            log.error("Could not create upload directory {}: {}", dir, e.getMessage());
            throw new ValidationException("Could not create image upload directory.");
        }

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String originalName = file.getOriginalFilename();
            String ext = (originalName != null && originalName.contains("."))
                    ? originalName.substring(originalName.lastIndexOf('.')).toLowerCase()
                    : ".jpg";

            String filename = UUID.randomUUID() + ext;
            Path target = dir.resolve(filename);

            try (java.io.InputStream in = file.getInputStream()) {
                Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                log.info("Saved image {} ({} bytes)", filename, file.getSize());
            } catch (IOException e) {
                log.error("Failed to save image {} to {}: {}", filename, target, e.getMessage());
                throw new ValidationException("Failed to save image: " + originalName);
            }

            result.add(ListingImage.builder()
                    .imageUrl("/images/" + filename)
                    .displayOrder(i)
                    .build());
        }
        return result;
    }

    /**
     * Maps a {@link Listing} entity to a {@link ListingResponse} DTO.
     */
    private ListingResponse toResponse(Listing l) {
        List<String> imageUrls = l.getImages().stream()
                .map(ListingImage::getImageUrl)
                .toList();

        String sellerDisplay = l.getSeller().getFirstName()
                + " " + l.getSeller().getLastName();

        return ListingResponse.builder()
                .id(l.getId())
                .title(l.getTitle())
                .description(l.getDescription())
                .category(l.getCategory() != null ? l.getCategory().toString() : null)
                .condition(l.getCondition() != null ? l.getCondition().toString() : null)
                .listingType(l.getListingType())
                .price(l.getPrice())
                .rentalPeriodUnit(l.getRentalPeriodUnit())
                .status(l.getStatus())
                .imageUrls(imageUrls)
                .sellerDisplayName(sellerDisplay)
                .sellerJoinDate(l.getSeller().getCreatedAt())
                .createdAt(l.getCreatedAt())
                .build();
    }

    /**
     * Returns a Pageable with size capped at {@code maxSize}.
     */
    private Pageable capPageSize(Pageable pageable, int maxSize) {
        if (pageable.getPageSize() <= maxSize) return pageable;
        return org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(), maxSize, pageable.getSort());
    }

    /**
     * Calculates the rental end date from the start date, duration, and period unit.
     */
    private LocalDate calculateEndDate(LocalDate start, int duration,
                                       Listing.RentalPeriodUnit unit) {
        return switch (unit) {
            case PER_DAY   -> start.plusDays(duration);
            case PER_WEEK  -> start.plusWeeks(duration);
            case PER_MONTH -> start.plusMonths(duration);
        };
    }

    /**
     * Maps a {@link Transaction} entity to a {@link TransactionResponse} DTO.
     */
    private TransactionResponse toTransactionResponse(Transaction t) {
        return TransactionResponse.builder()
                .transactionId(t.getId())
                .listingId(t.getListing().getId())
                .listingTitle(t.getListing().getTitle())
                .transactionType(t.getTransactionType())
                .pricePaid(t.getPricePaid())
                .transactionDate(t.getTransactionDate())
                .rentalStartDate(t.getRentalStartDate())
                .rentalEndDate(t.getRentalEndDate())
                .rentalDuration(t.getRentalDuration())
                .rentalPeriodUnit(t.getRentalPeriodUnit())
                .build();
    }
}
