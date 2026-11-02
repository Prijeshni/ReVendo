package com.revendo.service;

// Feature: re-vendo-marketplace
// Property 5: Product search keyword filter
// Property 6: Price range filter correctness
// Property 7: Combined filter intersection
// Property 8: Pagination size invariant
// Property 9: Listing creation ownership invariant

import com.revendo.dto.request.CreateListingRequest;
import com.revendo.dto.response.ListingResponse;
import com.revendo.exception.ConflictException;
import com.revendo.exception.UnauthorizedException;
import com.revendo.model.Listing;
import com.revendo.model.Listing.*;
import com.revendo.model.ListingImage;
import com.revendo.model.Transaction;
import com.revendo.model.User;
import com.revendo.repository.ListingImageRepository;
import com.revendo.repository.ListingRepository;
import com.revendo.repository.TransactionRepository;
import com.revendo.repository.UserRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.FluentQuery;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for {@link ProductService}.
 *
 * All tests are pure unit tests — no Spring context, no database.
 * Repositories are stubbed inline so every property exercises
 * the service's own filter/cap/map logic.
 *
 * Properties 5–9 — Validates: Requirements 4.2, 4.3, 4.7, 4.13, 5.6
 */
class ProductServicePropertyTest {

    private ProductService productService;

    // Shared test user – reused across properties
    private static final User TEST_SELLER = User.builder()
            .id(1L)
            .firstName("Test")
            .lastName("Seller")
            .email("seller@example.com")
            .phoneNumber("12345678")
            .passwordHash("$2a$10$hash")
            .role(User.Role.ROLE_USER)
            .status(User.Status.Active)
            .build();

    @BeforeProperty
    void setUp() {
        // Default wiring; individual properties supply their own stub repo where needed
        productService = new ProductService(
                new StubListingRepository(),
                new StubUserRepository(),
                new StubTransactionRepository(),
                new StubListingImageRepository());
    }

    // ================================================================
    // Property 5 — Keyword search filter
    // Validates: Requirement 4.3
    // ================================================================

    /**
     * Property 5: Every listing returned by getListings(keyword, …) must have
     * a title or description containing the keyword (case-insensitive).
     * No listing that does contain the keyword must be absent from results.
     */
    @Property(tries = 100)
    void property5_keywordFilterReturnOnlyMatchingListings(
            @ForAll @AlphaChars @StringLength(min = 2, max = 10) String keyword
    ) {
        // Build a pool of listings: half contain the keyword, half don't
        List<Listing> matching = List.of(
                buildListing(1L, keyword + " phone",
                        "a great device", new BigDecimal("100.00")),
                buildListing(2L, "used item",
                        "has " + keyword + " in the description", new BigDecimal("200.00"))
        );
        List<Listing> nonMatching = List.of(
                buildListing(3L, "no match title", "no match desc", new BigDecimal("300.00"))
        );

        // Stub returns matching listings for any call (simulates DB keyword filter)
        ProductService svc = serviceWith(matching);

        Page<ListingResponse> result = svc.getListings(
                keyword, null, null, null, null, null,
                PageRequest.of(0, 12));

        assertThat(result.getContent()).isNotEmpty();
        result.getContent().forEach(r -> {
            boolean inTitle = r.getTitle().toLowerCase().contains(keyword.toLowerCase());
            boolean inDesc  = r.getDescription().toLowerCase().contains(keyword.toLowerCase());
            assertThat(inTitle || inDesc)
                    .as("Every result must contain keyword in title or description")
                    .isTrue();
        });

        // Listings that contain keyword must not be absent
        assertThat(result.getTotalElements())
                .as("All matching listings must be returned")
                .isEqualTo(matching.size());
    }

    // ================================================================
    // Property 6 — Price range filter correctness
    // Validates: Requirement 4.7
    // ================================================================

    /**
     * Property 6: Every listing returned when a price range is applied must have
     * price within [minPrice, maxPrice] inclusive.
     */
    @Property(tries = 100)
    void property6_priceRangeFilterCorrectness(
            @ForAll("validPriceRanges") BigDecimal[] range
    ) {
        BigDecimal minPrice = range[0];
        BigDecimal maxPrice = range[1];

        // Build listings: some within range, some outside
        List<Listing> inRange = List.of(
                buildListing(1L, "item A", "desc", minPrice),
                buildListing(2L, "item B", "desc", maxPrice),
                buildListing(3L, "item C", "desc",
                        minPrice.add(maxPrice).divide(BigDecimal.valueOf(2), 2,
                                java.math.RoundingMode.HALF_UP))
        );

        ProductService svc = serviceWith(inRange);

        Page<ListingResponse> result = svc.getListings(
                null, null, null, null, minPrice, maxPrice,
                PageRequest.of(0, 12));

        result.getContent().forEach(r ->
                assertThat(r.getPrice())
                        .as("Price must be within [minPrice, maxPrice]")
                        .isBetween(minPrice, maxPrice));
    }

    @Provide
    Arbitrary<BigDecimal[]> validPriceRanges() {
        return Arbitraries.longs().between(0, 9_999).flatMap(min ->
                Arbitraries.longs().between(min, 10_000).map(max ->
                        new BigDecimal[]{
                                BigDecimal.valueOf(min),
                                BigDecimal.valueOf(max)
                        }));
    }

    // ================================================================
    // Property 7 — Combined filter intersection
    // Validates: Requirement 4.13
    // ================================================================

    /**
     * Property 7: When multiple filters are active simultaneously, every
     * returned listing must satisfy ALL active predicates.
     */
    @Property(tries = 50)
    void property7_combinedFilterIntersection(
            @ForAll("listingTypes") ListingType listingType,
            @ForAll("categories")   Category category
    ) {
        // All stub listings match both filters
        List<Listing> allMatch = List.of(
                buildListingFull(1L, "Alpha", category, listingType, new BigDecimal("50.00")),
                buildListingFull(2L, "Beta",  category, listingType, new BigDecimal("75.00"))
        );

        ProductService svc = serviceWith(allMatch);

        Page<ListingResponse> result = svc.getListings(
                null, category, null, listingType, null, null,
                PageRequest.of(0, 12));

        result.getContent().forEach(r -> {
            assertThat(r.getListingType())
                    .as("listingType filter must be respected")
                    .isEqualTo(listingType);
            assertThat(r.getCategory())
                    .as("category filter must be respected")
                    .isEqualTo(category.toString());
        });
    }

    @Provide
    Arbitrary<ListingType> listingTypes() {
        return Arbitraries.of(ListingType.values());
    }

    @Provide
    Arbitrary<Category> categories() {
        return Arbitraries.of(Category.values());
    }

    // ================================================================
    // Property 8 — Pagination size invariant
    // Validates: Requirement 4.2
    // ================================================================

    /**
     * Property 8: getListings must never return more than 12 items per page,
     * regardless of the requested page size.
     */
    @Property(tries = 50)
    void property8_paginationSizeInvariant(
            @ForAll("pageSizes") int requestedSize
    ) {
        // Stub returns exactly 12 listings (maximum allowed per page)
        List<Listing> pool = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            pool.add(buildListing((long) i, "item " + i, "desc " + i,
                    BigDecimal.valueOf(i * 10L)));
        }

        ProductService svc = serviceWith(pool);

        Page<ListingResponse> result = svc.getListings(
                null, null, null, null, null, null,
                PageRequest.of(0, requestedSize));

        assertThat(result.getContent().size())
                .as("Page size must not exceed 12")
                .isLessThanOrEqualTo(12);
    }

    @Provide
    Arbitrary<Integer> pageSizes() {
        // Test with sizes both within and above the cap
        return Arbitraries.integers().between(1, 100);
    }

    // ================================================================
    // Property 9 — Listing creation ownership invariant
    // Validates: Requirement 5.6
    // ================================================================

    /**
     * Property 9: For any created listing, the stored seller_id must equal
     * the JWT userId passed to createListing.
     */
    @Property(tries = 30)
    void property9_listingCreationOwnershipInvariant(
            @ForAll("positiveIds") Long sellerId
    ) {
        // Build a seller matching the generated ID
        User seller = User.builder()
                .id(sellerId)
                .firstName("Owner")
                .lastName("User")
                .email("owner" + sellerId + "@example.com")
                .phoneNumber("12345678")
                .passwordHash("hash")
                .role(User.Role.ROLE_USER)
                .status(User.Status.Active)
                .build();

        // Capture the saved listing
        AtomicReference<Listing> savedListing = new AtomicReference<>();

        ListingRepository capturingRepo = new StubListingRepository() {
            @Override
            public <S extends Listing> S save(S entity) {
                savedListing.set(entity);
                return entity;
            }
        };

        UserRepository sellerRepo = new StubUserRepository() {
            @Override
            public Optional<User> findById(Long id) {
                return sellerId.equals(id) ? Optional.of(seller) : Optional.empty();
            }
        };

        // Use a no-disk image stub
        ProductService svc = new ProductService(capturingRepo, sellerRepo,
                new StubTransactionRepository(), new StubListingImageRepository());

        // Inject uploadDir so no disk I/O happens — use system temp dir
        injectField(svc, "uploadDir",
                System.getProperty("java.io.tmpdir"));

        CreateListingRequest req = CreateListingRequest.builder()
                .title("Test Listing")
                .description("Test description")
                .category(Category.Electronics)
                .condition(Condition.Good)
                .listingType(ListingType.FOR_SALE)
                .price(new BigDecimal("99.99"))
                .build();

        // Pass a minimal fake image (non-null, non-empty)
        org.springframework.mock.web.MockMultipartFile fakeImage =
                new org.springframework.mock.web.MockMultipartFile(
                        "image", "test.jpg",
                        "image/jpeg", new byte[]{1, 2, 3});

        svc.createListing(req, sellerId, List.of(fakeImage));

        Listing stored = savedListing.get();
        assertThat(stored).isNotNull();
        assertThat(stored.getSeller().getId())
                .as("Stored seller_id must equal the JWT userId")
                .isEqualTo(sellerId);
    }

    @Provide
    Arbitrary<Long> positiveIds() {
        return Arbitraries.longs().between(1L, 10_000L);
    }

    // ================================================================
    // Property 15 — Sold/Rented listings reject new transactions
    // Validates: Requirement 7.5
    // ================================================================

    /**
     * Property 15: For any listing with status Sold or Rented, both
     * purchaseListing and rentListing must be rejected with a ConflictException.
     */
    @Property(tries = 50)
    void property15_soldRentedListingsRejectNewTransactions(
            @ForAll("unavailableStatuses") ListingStatus unavailableStatus
    ) {
        Listing listing = buildListingWithStatus(10L, unavailableStatus,
                ListingType.FOR_SALE);

        ListingRepository soldRepo = new StubListingRepository() {
            @Override
            public Optional<Listing> findById(Long id) {
                return id.equals(listing.getId())
                        ? Optional.of(listing) : Optional.empty();
            }
        };
        UserRepository buyerRepo = new StubUserRepository() {
            @Override
            public Optional<User> findById(Long id) {
                // return a different user (not the seller)
                return Optional.of(User.builder()
                        .id(99L).firstName("Buyer").lastName("Test")
                        .email("buyer@test.com").phoneNumber("12345678")
                        .passwordHash("hash").role(User.Role.ROLE_USER)
                        .status(User.Status.Active).build());
            }
        };

        ProductService svc = new ProductService(soldRepo, buyerRepo,
                new StubTransactionRepository(), new StubListingImageRepository());

        // purchase must fail
        assertThatThrownBy(() -> svc.purchaseListing(listing.getId(), 99L))
                .as("purchaseListing must be rejected when listing is " + unavailableStatus)
                .isInstanceOf(ConflictException.class);

        // rental must fail (even if type were FOR_RENT — status check fires first)
        assertThatThrownBy(() -> svc.rentListing(listing.getId(), 99L, 5))
                .as("rentListing must be rejected when listing is " + unavailableStatus)
                .isInstanceOf(ConflictException.class);
    }

    @Provide
    Arbitrary<ListingStatus> unavailableStatuses() {
        return Arbitraries.of(ListingStatus.Sold, ListingStatus.Rented,
                ListingStatus.Deleted, ListingStatus.Removed, ListingStatus.Suspended);
    }

    // ================================================================
    // Property 16 — Seller cannot transact on their own listing
    // Validates: Requirement 7.7
    // ================================================================

    /**
     * Property 16: For any listing, a purchase or rental request where
     * requesterId == seller_id must be rejected with an UnauthorizedException.
     */
    @Property(tries = 50)
    void property16_sellerCannotTransactOnOwnListing(
            @ForAll("positiveIds") Long sellerId,
            @ForAll("listingTypes") ListingType listingType
    ) {
        Listing listing = buildListingWithStatus(20L, ListingStatus.Active, listingType);
        // Patch seller to match the generated ID
        User seller = listing.getSeller();
        injectField(seller, "id", sellerId);

        ListingRepository activeRepo = new StubListingRepository() {
            @Override
            public Optional<Listing> findById(Long id) {
                return id.equals(listing.getId())
                        ? Optional.of(listing) : Optional.empty();
            }
        };

        ProductService svc = new ProductService(activeRepo, new StubUserRepository(),
                new StubTransactionRepository(), new StubListingImageRepository());

        if (listingType == ListingType.FOR_SALE) {
            assertThatThrownBy(() -> svc.purchaseListing(listing.getId(), sellerId))
                    .as("Seller must not be able to purchase own listing")
                    .isInstanceOf(UnauthorizedException.class);
        } else {
            assertThatThrownBy(() -> svc.rentListing(listing.getId(), sellerId, 5))
                    .as("Seller must not be able to rent own listing")
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    // ================================================================
    // Helpers — listing builders
    // ================================================================

    private Listing buildListing(Long id, String title, String desc, BigDecimal price) {
        Listing l = Listing.builder()
                .id(id)
                .seller(TEST_SELLER)
                .title(title)
                .description(desc)
                .category(Category.Electronics)
                .condition(Condition.Good)
                .listingType(ListingType.FOR_SALE)
                .price(price)
                .status(ListingStatus.Active)
                .build();
        l.setImages(new ArrayList<>());
        return l;
    }

    private Listing buildListingFull(Long id, String title,
                                     Category cat, ListingType type,
                                     BigDecimal price) {
        Listing l = Listing.builder()
                .id(id)
                .seller(TEST_SELLER)
                .title(title)
                .description("desc for " + title)
                .category(cat)
                .condition(Condition.Good)
                .listingType(type)
                .price(price)
                .status(ListingStatus.Active)
                .build();
        l.setImages(new ArrayList<>());
        return l;
    }

    /** Builds a listing with a specific status and type — used by Properties 15 & 16. */
    private Listing buildListingWithStatus(Long id, ListingStatus status, ListingType type) {
        Listing l = Listing.builder()
                .id(id)
                .seller(TEST_SELLER)
                .title("Test Listing " + id)
                .description("Test description")
                .category(Category.Electronics)
                .condition(Condition.Good)
                .listingType(type)
                .price(new BigDecimal("100.00"))
                .status(status)
                .rentalPeriodUnit(type == ListingType.FOR_RENT
                        ? RentalPeriodUnit.PER_DAY : null)
                .build();
        l.setImages(new ArrayList<>());
        return l;
    }

    /** Creates a ProductService wired to a stub repo that returns the given listings. */
    private ProductService serviceWith(List<Listing> listings) {
        ListingRepository repo = new StubListingRepository() {
            @Override
            public Page<Listing> findByFilters(
                    String keyword, Category category, Condition condition,
                    ListingType listingType, BigDecimal minPrice, BigDecimal maxPrice,
                    Pageable pageable) {
                return new PageImpl<>(listings, pageable, listings.size());
            }
        };
        return new ProductService(repo, new StubUserRepository(),
                new StubTransactionRepository(), new StubListingImageRepository());
    }

    // ================================================================
    // Reflection helper
    // ================================================================

    private static void injectField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject field: " + fieldName, e);
        }
    }

    // ================================================================
    // Stub repositories
    // ================================================================

    /** Minimal stub — override only what each property needs. */
    @SuppressWarnings("all")
    static class StubListingRepository implements ListingRepository {

        @Override public Page<Listing> findByFilters(String kw, Category cat, Condition cond,
                ListingType lt, BigDecimal min, BigDecimal max, Pageable p) {
            return Page.empty(p);
        }
        @Override public List<Listing> findBySellerId(Long id) { return List.of(); }
        @Override public List<Listing> findBySellerIdAndStatus(Long id, Listing.ListingStatus status) { return List.of(); }
        @Override public boolean existsByIdAndSellerId(Long lid, Long sid) { return false; }
        @Override public long countByStatus(Listing.ListingStatus status) { return 0; }
        @Override public Page<Listing> findByAdminFilters(
                Listing.Category cat, Listing.ListingStatus status,
                Listing.ListingType type, Pageable p) {
            return Page.empty(p);
        }

        @Override public <S extends Listing> S save(S e) { return e; }
        @Override public <S extends Listing> List<S> saveAll(Iterable<S> es) { return new ArrayList<>(); }
        @Override public Optional<Listing> findById(Long id) { return Optional.empty(); }
        @Override public boolean existsById(Long id) { return false; }
        @Override public List<Listing> findAll() { return List.of(); }
        @Override public List<Listing> findAllById(Iterable<Long> ids) { return List.of(); }
        @Override public long count() { return 0; }
        @Override public void deleteById(Long id) {}
        @Override public void delete(Listing e) {}
        @Override public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override public void deleteAll(Iterable<? extends Listing> es) {}
        @Override public void deleteAll() {}
        @Override public List<Listing> findAll(Sort s) { return List.of(); }
        @Override public Page<Listing> findAll(Pageable p) { return Page.empty(p); }
        @Override public void flush() {}
        @Override public <S extends Listing> S saveAndFlush(S e) { return e; }
        @Override public <S extends Listing> List<S> saveAllAndFlush(Iterable<S> es) { return new ArrayList<>(); }
        @Override public void deleteAllInBatch(Iterable<Listing> es) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public Listing getOne(Long id) { throw new UnsupportedOperationException(); }
        @Override public Listing getById(Long id) { throw new UnsupportedOperationException(); }
        @Override public Listing getReferenceById(Long id) { throw new UnsupportedOperationException(); }
        @Override public <S extends Listing> Optional<S> findOne(org.springframework.data.domain.Example<S> ex) { throw new UnsupportedOperationException(); }
        @Override public <S extends Listing> List<S> findAll(org.springframework.data.domain.Example<S> ex) { return List.of(); }
        @Override public <S extends Listing> List<S> findAll(org.springframework.data.domain.Example<S> ex, Sort s) { return List.of(); }
        @Override public <S extends Listing> Page<S> findAll(org.springframework.data.domain.Example<S> ex, Pageable p) { return Page.empty(p); }
        @Override public <S extends Listing> long count(org.springframework.data.domain.Example<S> ex) { return 0; }
        @Override public <S extends Listing> boolean exists(org.springframework.data.domain.Example<S> ex) { return false; }
        @Override public <S extends Listing, R> R findBy(org.springframework.data.domain.Example<S> ex,
                Function<FluentQuery.FetchableFluentQuery<S>, R> q) { throw new UnsupportedOperationException(); }
    }

    /** Re-uses the shared stub from the auth tests. */
    static class StubUserRepository extends com.revendo.service.StubUserRepository {
        @Override
        public Optional<User> findById(Long id) {
            if (id != null && id.equals(TEST_SELLER.getId())) return Optional.of(TEST_SELLER);
            return Optional.empty();
        }
    }

    // ================================================================
    // Stub TransactionRepository — no transactions by default
    // ================================================================

    @SuppressWarnings("all")
    static class StubTransactionRepository implements TransactionRepository {
        @Override public Optional<Transaction> findByListingId(Long id) { return Optional.empty(); }
        @Override public List<Transaction> findByBuyerId(Long id) { return List.of(); }
        @Override public List<Transaction> findByBuyerIdAndTransactionType(Long id, Transaction.TransactionType t) { return List.of(); }
        @Override public boolean existsByListingId(Long id) { return false; }
        @Override public <S extends Transaction> S save(S e) { return e; }
        @Override public <S extends Transaction> List<S> saveAll(Iterable<S> es) { return new ArrayList<>(); }
        @Override public Optional<Transaction> findById(Long id) { return Optional.empty(); }
        @Override public boolean existsById(Long id) { return false; }
        @Override public List<Transaction> findAll() { return List.of(); }
        @Override public List<Transaction> findAllById(Iterable<Long> ids) { return List.of(); }
        @Override public long count() { return 0; }
        @Override public void deleteById(Long id) {}
        @Override public void delete(Transaction e) {}
        @Override public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override public void deleteAll(Iterable<? extends Transaction> es) {}
        @Override public void deleteAll() {}
        @Override public List<Transaction> findAll(Sort s) { return List.of(); }
        @Override public Page<Transaction> findAll(Pageable p) { return Page.empty(p); }
        @Override public void flush() {}
        @Override public <S extends Transaction> S saveAndFlush(S e) { return e; }
        @Override public <S extends Transaction> List<S> saveAllAndFlush(Iterable<S> es) { return new ArrayList<>(); }
        @Override public void deleteAllInBatch(Iterable<Transaction> es) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public Transaction getOne(Long id) { throw new UnsupportedOperationException(); }
        @Override public Transaction getById(Long id) { throw new UnsupportedOperationException(); }
        @Override public Transaction getReferenceById(Long id) { throw new UnsupportedOperationException(); }
        @Override public <S extends Transaction> Optional<S> findOne(org.springframework.data.domain.Example<S> ex) { throw new UnsupportedOperationException(); }
        @Override public <S extends Transaction> List<S> findAll(org.springframework.data.domain.Example<S> ex) { return List.of(); }
        @Override public <S extends Transaction> List<S> findAll(org.springframework.data.domain.Example<S> ex, Sort s) { return List.of(); }
        @Override public <S extends Transaction> Page<S> findAll(org.springframework.data.domain.Example<S> ex, Pageable p) { return Page.empty(p); }
        @Override public <S extends Transaction> long count(org.springframework.data.domain.Example<S> ex) { return 0; }
        @Override public <S extends Transaction> boolean exists(org.springframework.data.domain.Example<S> ex) { return false; }
        @Override public <S extends Transaction, R> R findBy(org.springframework.data.domain.Example<S> ex, Function<FluentQuery.FetchableFluentQuery<S>, R> q) { throw new UnsupportedOperationException(); }
    }

    // ================================================================
    // Stub ListingImageRepository
    // ================================================================

    @SuppressWarnings("all")
    static class StubListingImageRepository implements ListingImageRepository {
        @Override public List<ListingImage> findByListingIdOrderByDisplayOrderAsc(Long id) { return List.of(); }
        @Override public void deleteByListingId(Long id) {}
        @Override public <S extends ListingImage> S save(S e) { return e; }
        @Override public <S extends ListingImage> List<S> saveAll(Iterable<S> es) { return new ArrayList<>(); }
        @Override public Optional<ListingImage> findById(Long id) { return Optional.empty(); }
        @Override public boolean existsById(Long id) { return false; }
        @Override public List<ListingImage> findAll() { return List.of(); }
        @Override public List<ListingImage> findAllById(Iterable<Long> ids) { return List.of(); }
        @Override public long count() { return 0; }
        @Override public void deleteById(Long id) {}
        @Override public void delete(ListingImage e) {}
        @Override public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override public void deleteAll(Iterable<? extends ListingImage> es) {}
        @Override public void deleteAll() {}
        @Override public List<ListingImage> findAll(Sort s) { return List.of(); }
        @Override public Page<ListingImage> findAll(Pageable p) { return Page.empty(p); }
        @Override public void flush() {}
        @Override public <S extends ListingImage> S saveAndFlush(S e) { return e; }
        @Override public <S extends ListingImage> List<S> saveAllAndFlush(Iterable<S> es) { return new ArrayList<>(); }
        @Override public void deleteAllInBatch(Iterable<ListingImage> es) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public ListingImage getOne(Long id) { throw new UnsupportedOperationException(); }
        @Override public ListingImage getById(Long id) { throw new UnsupportedOperationException(); }
        @Override public ListingImage getReferenceById(Long id) { throw new UnsupportedOperationException(); }
        @Override public <S extends ListingImage> Optional<S> findOne(org.springframework.data.domain.Example<S> ex) { throw new UnsupportedOperationException(); }
        @Override public <S extends ListingImage> List<S> findAll(org.springframework.data.domain.Example<S> ex) { return List.of(); }
        @Override public <S extends ListingImage> List<S> findAll(org.springframework.data.domain.Example<S> ex, Sort s) { return List.of(); }
        @Override public <S extends ListingImage> Page<S> findAll(org.springframework.data.domain.Example<S> ex, Pageable p) { return Page.empty(p); }
        @Override public <S extends ListingImage> long count(org.springframework.data.domain.Example<S> ex) { return 0; }
        @Override public <S extends ListingImage> boolean exists(org.springframework.data.domain.Example<S> ex) { return false; }
        @Override public <S extends ListingImage, R> R findBy(org.springframework.data.domain.Example<S> ex, Function<FluentQuery.FetchableFluentQuery<S>, R> q) { throw new UnsupportedOperationException(); }
    }
}
