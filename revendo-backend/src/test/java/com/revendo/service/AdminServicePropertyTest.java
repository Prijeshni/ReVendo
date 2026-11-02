package com.revendo.service;

// Feature: re-vendo-marketplace
// Property 17: Admin suspension cascades to listings
// Property 18: Admin reactivation restores suspended listings

import com.revendo.model.Listing;
import com.revendo.model.Listing.ListingStatus;
import com.revendo.model.Offer;
import com.revendo.model.User;
import com.revendo.repository.ListingRepository;
import com.revendo.repository.OfferRepository;
import com.revendo.repository.TransactionRepository;
import com.revendo.repository.UserRepository;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.FluentQuery;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for {@link AdminService}.
 *
 * Pure unit tests — no Spring context, no database.
 *
 * Property 17 — Validates: Requirement 13.5
 * Property 18 — Validates: Requirement 13.6
 */
class AdminServicePropertyTest {

    @BeforeProperty
    void setUp() {}

    // ================================================================
    // Property 17 — Admin suspension cascades to listings
    // Validates: Requirement 13.5
    // ================================================================

    /**
     * For any user with N (1–20) Active listings, calling suspendUser must:
     * 1. Set the user's status to Suspended.
     * 2. Set ALL N Active listings to Suspended.
     * 3. Leave no Active listings for that user.
     */
    @Property(tries = 100)
    void property17_suspensionCascadesToListings(
            @ForAll("listingCounts") int n
    ) {
        User user = buildUser(1L, User.Status.Active);
        List<Listing> listings = buildListings(user, n, ListingStatus.Active);

        // Mutable stores
        List<User>    userStore    = new CopyOnWriteArrayList<>(List.of(user));
        List<Listing> listingStore = new CopyOnWriteArrayList<>(listings);

        AdminService svc = buildAdminService(userStore, listingStore);

        svc.suspendUser(user.getId());

        // Assert user is suspended
        assertThat(userStore.get(0).getStatus())
                .as("User status must be Suspended after suspendUser")
                .isEqualTo(User.Status.Suspended);

        // Assert every listing is now Suspended
        assertThat(listingStore)
                .as("Every listing must be Suspended after suspendUser")
                .allMatch(l -> l.getStatus() == ListingStatus.Suspended);

        // Assert no Active listing remains
        assertThat(listingStore.stream()
                .filter(l -> l.getStatus() == ListingStatus.Active)
                .count())
                .as("No Active listing must remain after suspendUser")
                .isZero();
    }

    // ================================================================
    // Property 18 — Admin reactivation restores suspended listings
    // Validates: Requirement 13.6
    // ================================================================

    /**
     * For a Suspended user with N (1–20) Suspended listings, calling
     * reactivateUser must:
     * 1. Set the user's status to Active.
     * 2. Set ALL N Suspended listings back to Active.
     */
    @Property(tries = 100)
    void property18_reactivationRestoresListings(
            @ForAll("listingCounts") int n
    ) {
        User user = buildUser(2L, User.Status.Suspended);
        List<Listing> listings = buildListings(user, n, ListingStatus.Suspended);

        List<User>    userStore    = new CopyOnWriteArrayList<>(List.of(user));
        List<Listing> listingStore = new CopyOnWriteArrayList<>(listings);

        AdminService svc = buildAdminService(userStore, listingStore);

        svc.reactivateUser(user.getId());

        // Assert user is Active
        assertThat(userStore.get(0).getStatus())
                .as("User status must be Active after reactivateUser")
                .isEqualTo(User.Status.Active);

        // Assert every listing is now Active
        assertThat(listingStore)
                .as("Every listing must be Active after reactivateUser")
                .allMatch(l -> l.getStatus() == ListingStatus.Active);
    }

    @Provide
    Arbitrary<Integer> listingCounts() {
        return Arbitraries.integers().between(1, 20);
    }

    // ================================================================
    // Service builder
    // ================================================================

    private AdminService buildAdminService(
            List<User> userStore, List<Listing> listingStore) {

        UserRepository userRepo = new StubUserRepo(userStore);
        ListingRepository listingRepo = new StubListingRepo(listingStore);
        TransactionRepository txRepo = new StubTransactionRepo();
        OfferRepository offerRepo = new StubOfferRepo();

        return new AdminService(userRepo, listingRepo, txRepo, offerRepo);
    }

    // ================================================================
    // Model builders
    // ================================================================

    private static User buildUser(Long id, User.Status status) {
        return User.builder()
                .id(id).firstName("Test").lastName("User")
                .email("user" + id + "@test.com").phoneNumber("12345678")
                .passwordHash("hash").role(User.Role.ROLE_USER)
                .status(status).build();
    }

    private static List<Listing> buildListings(User seller, int n, ListingStatus status) {
        return IntStream.rangeClosed(1, n)
                .mapToObj(i -> {
                    Listing l = Listing.builder()
                            .id((long) i).seller(seller)
                            .title("Listing " + i).description("desc")
                            .category(Listing.Category.Electronics)
                            .condition(Listing.Condition.Good)
                            .listingType(Listing.ListingType.FOR_SALE)
                            .price(new BigDecimal("50.00"))
                            .status(status)
                            .build();
                    l.setImages(new ArrayList<>());
                    return l;
                })
                .collect(Collectors.toList());
    }

    // ================================================================
    // Stub UserRepository
    // ================================================================

    @SuppressWarnings("all")
    static class StubUserRepo extends com.revendo.service.StubUserRepository {

        private final List<User> store;

        StubUserRepo(List<User> store) { this.store = store; }

        @Override
        public Optional<User> findById(Long id) {
            return store.stream().filter(u -> u.getId().equals(id)).findFirst();
        }

        @Override
        public <S extends User> S save(S entity) {
            store.replaceAll(u -> u.getId().equals(entity.getId()) ? entity : u);
            return entity;
        }

        @Override
        public long count() { return store.size(); }
    }

    // ================================================================
    // Stub ListingRepository
    // ================================================================

    @SuppressWarnings("all")
    static class StubListingRepo extends ProductServicePropertyTest.StubListingRepository {

        private final List<Listing> store;

        StubListingRepo(List<Listing> store) { this.store = store; }

        @Override
        public Optional<Listing> findById(Long id) {
            return store.stream().filter(l -> l.getId().equals(id)).findFirst();
        }

        @Override
        public List<Listing> findBySellerIdAndStatus(Long sellerId, ListingStatus status) {
            return store.stream()
                    .filter(l -> l.getSeller().getId().equals(sellerId)
                              && l.getStatus() == status)
                    .collect(Collectors.toList());
        }

        @Override
        public <S extends Listing> S save(S entity) {
            store.replaceAll(l -> l.getId().equals(entity.getId()) ? entity : l);
            return entity;
        }

        @Override
        public <S extends Listing> List<S> saveAll(Iterable<S> entities) {
            entities.forEach(e ->
                    store.replaceAll(l -> l.getId().equals(e.getId()) ? e : l));
            List<S> result = new ArrayList<>();
            entities.forEach(result::add);
            return result;
        }

        @Override
        public Page<Listing> findByAdminFilters(
                Listing.Category cat, ListingStatus status,
                Listing.ListingType type, Pageable pageable) {
            List<Listing> filtered = store.stream()
                    .filter(l -> (cat    == null || l.getCategory()    == cat)
                              && (status == null || l.getStatus()      == status)
                              && (type   == null || l.getListingType() == type))
                    .collect(Collectors.toList());
            return new PageImpl<>(filtered, pageable, filtered.size());
        }

        @Override
        public long count() { return store.size(); }

        @Override
        public long countByStatus(ListingStatus status) {
            return store.stream().filter(l -> l.getStatus() == status).count();
        }
    }

    // ================================================================
    // Stub TransactionRepository (no-op — not needed for Properties 17/18)
    // ================================================================

    @SuppressWarnings("all")
    static class StubTransactionRepo
            extends ProductServicePropertyTest.StubTransactionRepository {
        @Override public long count() { return 0; }
    }

    // ================================================================
    // Stub OfferRepository (no-op)
    // ================================================================

    @SuppressWarnings("all")
    static class StubOfferRepo extends NegotiationServicePropertyTest.StubOfferRepository {
        StubOfferRepo() { super(new ArrayList<>()); }
    }
}
