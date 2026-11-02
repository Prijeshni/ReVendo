package com.revendo.service;

// Feature: re-vendo-marketplace
// Property 12: Wishlist idempotence — no duplicates

import com.revendo.model.Listing;
import com.revendo.model.User;
import com.revendo.model.WishlistItem;
import com.revendo.repository.ListingRepository;
import com.revendo.repository.UserRepository;
import com.revendo.repository.WishlistItemRepository;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for {@link WishlistService}.
 *
 * Pure unit tests — no Spring context, no database.
 *
 * Property 12 — Validates: Requirements 10.8, 10.2
 */
class WishlistServicePropertyTest {

    // ---------- canonical test fixtures ----------

    private static final User BUYER = User.builder()
            .id(1L).firstName("Test").lastName("User")
            .email("user@test.com").phoneNumber("12345678")
            .passwordHash("hash").role(User.Role.ROLE_USER)
            .status(User.Status.Active).build();

    private static final Listing LISTING = buildListing(10L);

    // ---------- service under test ----------

    private WishlistService service;

    @BeforeProperty
    void setUp() {
        // Default setup; individual properties build their own stub where needed
        service = buildService(new ArrayList<>(), new AtomicInteger(0));
    }

    // ================================================================
    // Property 12 — Wishlist idempotence — no duplicates
    // Validates: Requirements 10.8, 10.2
    // ================================================================

    /**
     * Calling addToWishlist N times (2–10) for the same (userId, listingId) pair
     * must result in exactly one WishlistItem being saved — subsequent calls are no-ops.
     */
    @Property(tries = 100)
    void property12_addToWishlistIsIdempotent(
            @ForAll("repeatCounts") int n
    ) {
        AtomicInteger saveCount = new AtomicInteger(0);
        List<WishlistItem> store = new ArrayList<>();

        WishlistService svc = buildService(store, saveCount);

        // Call addToWishlist N times with the same pair
        for (int i = 0; i < n; i++) {
            svc.addToWishlist(BUYER.getId(), LISTING.getId());
        }

        // save() must have been called exactly once regardless of N
        assertThat(saveCount.get())
                .as("save() must be called exactly once for N=" + n + " add calls")
                .isEqualTo(1);

        // The store must contain exactly one entry
        assertThat(store)
                .as("Wishlist store must contain exactly one item")
                .hasSize(1);
    }

    @Provide
    Arbitrary<Integer> repeatCounts() {
        // N between 2 and 10 (1 would be trivially true)
        return Arbitraries.integers().between(2, 10);
    }

    // ================================================================
    // Helpers
    // ================================================================

    private WishlistService buildService(List<WishlistItem> store, AtomicInteger saveCount) {

        WishlistItemRepository wishlistRepo = new StubWishlistItemRepository(store, saveCount);

        UserRepository userRepo = new StubUserRepository() {
            @Override
            public Optional<User> findById(Long id) {
                return BUYER.getId().equals(id) ? Optional.of(BUYER) : Optional.empty();
            }
        };

        ListingRepository listingRepo = new ProductServicePropertyTest.StubListingRepository() {
            @Override
            public Optional<Listing> findById(Long id) {
                return LISTING.getId().equals(id) ? Optional.of(LISTING) : Optional.empty();
            }
        };

        return new WishlistService(wishlistRepo, listingRepo, userRepo);
    }

    private static Listing buildListing(Long id) {
        Listing l = Listing.builder()
                .id(id).seller(BUYER)
                .title("Listed Item").description("desc")
                .category(Listing.Category.Electronics)
                .condition(Listing.Condition.Good)
                .listingType(Listing.ListingType.FOR_SALE)
                .price(new BigDecimal("50.00"))
                .status(Listing.ListingStatus.Active)
                .build();
        l.setImages(new ArrayList<>());
        return l;
    }

    // ================================================================
    // Stub WishlistItemRepository
    // ================================================================

    @SuppressWarnings("all")
    static class StubWishlistItemRepository implements WishlistItemRepository {

        private final List<WishlistItem> store;
        private final AtomicInteger      saveCount;

        StubWishlistItemRepository(List<WishlistItem> store, AtomicInteger saveCount) {
            this.store     = store;
            this.saveCount = saveCount;
        }

        @Override
        public Optional<WishlistItem> findByUserIdAndListingId(Long userId, Long listingId) {
            return store.stream()
                    .filter(w -> w.getUser().getId().equals(userId)
                              && w.getListing().getId().equals(listingId))
                    .findFirst();
        }

        @Override
        public List<WishlistItem> findByUserId(Long userId) {
            return store.stream()
                    .filter(w -> w.getUser().getId().equals(userId))
                    .collect(Collectors.toList());
        }

        @Override
        public boolean existsByUserIdAndListingId(Long userId, Long listingId) {
            return store.stream().anyMatch(
                    w -> w.getUser().getId().equals(userId)
                      && w.getListing().getId().equals(listingId));
        }

        @Override
        public void deleteByUserIdAndListingId(Long userId, Long listingId) {
            store.removeIf(w -> w.getUser().getId().equals(userId)
                             && w.getListing().getId().equals(listingId));
        }

        @Override
        public <S extends WishlistItem> S save(S entity) {
            saveCount.incrementAndGet();
            store.add(entity);
            return entity;
        }

        // ----- boilerplate -----
        @Override public <S extends WishlistItem> List<S> saveAll(Iterable<S> es) { return new ArrayList<>(); }
        @Override public Optional<WishlistItem> findById(Long id) { return Optional.empty(); }
        @Override public boolean existsById(Long id) { return false; }
        @Override public List<WishlistItem> findAll() { return new ArrayList<>(store); }
        @Override public List<WishlistItem> findAllById(Iterable<Long> ids) { return List.of(); }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(Long id) {}
        @Override public void delete(WishlistItem e) {}
        @Override public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override public void deleteAll(Iterable<? extends WishlistItem> es) {}
        @Override public void deleteAll() {}
        @Override public List<WishlistItem> findAll(Sort s) { return List.of(); }
        @Override public Page<WishlistItem> findAll(Pageable p) { return Page.empty(p); }
        @Override public void flush() {}
        @Override public <S extends WishlistItem> S saveAndFlush(S e) { return save(e); }
        @Override public <S extends WishlistItem> List<S> saveAllAndFlush(Iterable<S> es) { return new ArrayList<>(); }
        @Override public void deleteAllInBatch(Iterable<WishlistItem> es) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public WishlistItem getOne(Long id) { throw new UnsupportedOperationException(); }
        @Override public WishlistItem getById(Long id) { throw new UnsupportedOperationException(); }
        @Override public WishlistItem getReferenceById(Long id) { throw new UnsupportedOperationException(); }
        @Override public <S extends WishlistItem> Optional<S> findOne(Example<S> ex) { throw new UnsupportedOperationException(); }
        @Override public <S extends WishlistItem> List<S> findAll(Example<S> ex) { return List.of(); }
        @Override public <S extends WishlistItem> List<S> findAll(Example<S> ex, Sort s) { return List.of(); }
        @Override public <S extends WishlistItem> Page<S> findAll(Example<S> ex, Pageable p) { return Page.empty(p); }
        @Override public <S extends WishlistItem> long count(Example<S> ex) { return 0; }
        @Override public <S extends WishlistItem> boolean exists(Example<S> ex) { return false; }
        @Override public <S extends WishlistItem, R> R findBy(Example<S> ex, Function<FluentQuery.FetchableFluentQuery<S>, R> q) { throw new UnsupportedOperationException(); }
    }

    // reuse base stub
    static class StubUserRepository extends com.revendo.service.StubUserRepository {}
}
