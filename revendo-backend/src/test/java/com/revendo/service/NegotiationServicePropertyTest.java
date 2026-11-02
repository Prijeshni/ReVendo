package com.revendo.service;

// Feature: re-vendo-marketplace
// Property 10: Offer duplicate prevention
// Property 11: Negotiation round-count enforcement

import com.revendo.exception.ConflictException;
import com.revendo.model.Listing;
import com.revendo.model.Notification;
import com.revendo.model.Offer;
import com.revendo.model.Offer.OfferStatus;
import com.revendo.model.User;
import com.revendo.repository.ListingRepository;
import com.revendo.repository.NotificationRepository;
import com.revendo.repository.OfferRepository;
import com.revendo.repository.UserRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for {@link NegotiationService}.
 *
 * Pure unit tests — no Spring context, no database.
 * All repositories are stubbed inline.
 *
 * Property 10 — Validates: Requirement 8.12
 * Property 11 — Validates: Requirement 8.11
 */
class NegotiationServicePropertyTest {

    // ---------- canonical test fixtures ----------

    private static final User SELLER = User.builder()
            .id(1L).firstName("Alice").lastName("Seller")
            .email("seller@test.com").phoneNumber("11111111")
            .passwordHash("hash").role(User.Role.ROLE_USER)
            .status(User.Status.Active).build();

    private static final User BUYER = User.builder()
            .id(2L).firstName("Bob").lastName("Buyer")
            .email("buyer@test.com").phoneNumber("22222222")
            .passwordHash("hash").role(User.Role.ROLE_USER)
            .status(User.Status.Active).build();

    private static final Listing LISTING = buildActiveListing(10L, SELLER);

    // ---------- service under test ----------

    private NegotiationService service;

    @BeforeProperty
    void setUp() {
        service = buildService(new ArrayList<>());
    }

    // ================================================================
    // Property 10 — Offer duplicate prevention
    // Validates: Requirement 8.12
    // ================================================================

    /**
     * For any (listingId, buyerId) pair that already has a Pending or Countered
     * offer, a second submitOffer call must throw ConflictException.
     */
    @Property(tries = 100)
    void property10_duplicateOfferPrevention(
            @ForAll @AlphaChars @StringLength(min = 2, max = 10) String ignored
    ) {
        // Arrange: an existing Pending offer for the same pair
        Offer existingOffer = buildOffer(1L, LISTING, BUYER, OfferStatus.Pending, 0);

        List<Offer> store = new ArrayList<>();
        store.add(existingOffer);

        NegotiationService svc = buildService(store);

        // Act + Assert: second call must be rejected
        assertThatThrownBy(() ->
                svc.submitOffer(LISTING.getId(), BUYER.getId(), new BigDecimal("80.00")))
                .as("A second Pending offer for the same listing+buyer must be rejected")
                .isInstanceOf(ConflictException.class);
    }

    /**
     * A Countered offer also prevents a new offer being submitted.
     */
    @Property(tries = 50)
    void property10b_counteredOfferAlsoPreventsNewOffer(
            @ForAll @AlphaChars @StringLength(min = 1, max = 5) String ignored
    ) {
        Offer counteredOffer = buildOffer(2L, LISTING, BUYER, OfferStatus.Countered, 1);

        NegotiationService svc = buildService(List.of(counteredOffer));

        assertThatThrownBy(() ->
                svc.submitOffer(LISTING.getId(), BUYER.getId(), new BigDecimal("90.00")))
                .as("A Countered offer must also prevent a new offer submission")
                .isInstanceOf(ConflictException.class);
    }

    // ================================================================
    // Property 11 — Negotiation round-count enforcement
    // Validates: Requirement 8.11
    // ================================================================

    /**
     * After 5 counter rounds, the 6th counterOffer call must throw ConflictException.
     * The seller can still accept or reject.
     */
    @Property(tries = 50)
    void property11_roundCountEnforcement(
            @ForAll("offerAmounts") BigDecimal offerAmount,
            @ForAll("offerAmounts") BigDecimal counterAmount
    ) {
        // Build an offer already at round 5
        AtomicReference<Offer> stored = new AtomicReference<>(
                buildOffer(5L, LISTING, BUYER, OfferStatus.Pending, 5));

        NegotiationService svc = buildMutableService(stored);

        // 6th counter must be rejected (Requirement 8.11)
        assertThatThrownBy(() ->
                svc.counterOffer(stored.get().getId(), SELLER.getId(), counterAmount))
                .as("counterOffer at round 5 must throw ConflictException")
                .isInstanceOf(ConflictException.class);

        // Accept is still allowed after max rounds
        stored.set(buildOffer(5L, LISTING, BUYER, OfferStatus.Pending, 5));
        NegotiationService svc2 = buildMutableService(stored);
        var accepted = svc2.acceptOffer(stored.get().getId(), SELLER.getId());
        assertThat(accepted.getStatus())
                .as("Accept must still work after max rounds")
                .isEqualTo(OfferStatus.Accepted);

        // Reject is also still allowed
        stored.set(buildOffer(5L, LISTING, BUYER, OfferStatus.Pending, 5));
        NegotiationService svc3 = buildMutableService(stored);
        var rejected = svc3.rejectOffer(stored.get().getId(), SELLER.getId());
        assertThat(rejected.getStatus())
                .as("Reject must still work after max rounds")
                .isEqualTo(OfferStatus.Rejected);
    }

    @Provide
    Arbitrary<BigDecimal> offerAmounts() {
        return Arbitraries.longs().between(1, 10_000)
                .map(BigDecimal::valueOf);
    }

    // ================================================================
    // Helpers
    // ================================================================

    private static Listing buildActiveListing(Long id, User seller) {
        Listing l = Listing.builder()
                .id(id).seller(seller).title("Test Listing")
                .description("desc").category(Listing.Category.Electronics)
                .condition(Listing.Condition.Good)
                .listingType(Listing.ListingType.FOR_SALE)
                .price(new BigDecimal("100.00"))
                .status(Listing.ListingStatus.Active)
                .build();
        l.setImages(new ArrayList<>());
        return l;
    }

    private static Offer buildOffer(Long id, Listing listing, User buyer,
                                    OfferStatus status, int roundCount) {
        return Offer.builder()
                .id(id).listing(listing).buyer(buyer)
                .offerAmount(new BigDecimal("80.00"))
                .status(status).roundCount(roundCount)
                .build();
    }

    // Builds a service where the offer store is a fixed list (for submit duplicate test)
    private NegotiationService buildService(List<Offer> offerStore) {
        OfferRepository offerRepo = new StubOfferRepository(offerStore);
        return new NegotiationService(offerRepo,
                buildListingRepo(), buildUserRepo(), new StubNotificationRepository());
    }

    // Builds a service backed by a single mutable offer (for round-count test)
    private NegotiationService buildMutableService(AtomicReference<Offer> offerRef) {
        OfferRepository mutableRepo = new StubOfferRepository(new ArrayList<>()) {
            @Override
            public Optional<Offer> findById(Long id) {
                return id.equals(offerRef.get().getId())
                        ? Optional.of(offerRef.get()) : Optional.empty();
            }
            @Override
            public <S extends Offer> S save(S entity) {
                offerRef.set(entity);
                return entity;
            }
        };
        return new NegotiationService(mutableRepo,
                buildListingRepo(), buildUserRepo(), new StubNotificationRepository());
    }

    private ListingRepository buildListingRepo() {
        return new StubListingRepository() {
            @Override
            public Optional<Listing> findById(Long id) {
                return id.equals(LISTING.getId())
                        ? Optional.of(LISTING) : Optional.empty();
            }
        };
    }

    private UserRepository buildUserRepo() {
        return new StubUserRepository() {
            @Override
            public Optional<User> findById(Long id) {
                if (SELLER.getId().equals(id)) return Optional.of(SELLER);
                if (BUYER.getId().equals(id))  return Optional.of(BUYER);
                return Optional.empty();
            }
        };
    }

    // ================================================================
    // Stub OfferRepository
    // ================================================================

    @SuppressWarnings("all")
    static class StubOfferRepository implements OfferRepository {

        private final List<Offer> store;

        StubOfferRepository(List<Offer> store) {
            this.store = store;
        }

        @Override
        public List<Offer> findByListingId(Long id) {
            return store.stream()
                    .filter(o -> o.getListing().getId().equals(id))
                    .collect(Collectors.toList());
        }

        @Override
        public List<Offer> findByBuyerId(Long id) {
            return store.stream()
                    .filter(o -> o.getBuyer().getId().equals(id))
                    .collect(Collectors.toList());
        }

        @Override
        public Optional<Offer> findByListingIdAndBuyerIdAndStatusIn(
                Long listingId, Long buyerId, List<OfferStatus> statuses) {
            return store.stream()
                    .filter(o -> o.getListing().getId().equals(listingId)
                              && o.getBuyer().getId().equals(buyerId)
                              && statuses.contains(o.getStatus()))
                    .findFirst();
        }

        @Override
        public List<Offer> findByListingIdAndStatusIn(Long listingId,
                List<OfferStatus> statuses) {
            return store.stream()
                    .filter(o -> o.getListing().getId().equals(listingId)
                              && statuses.contains(o.getStatus()))
                    .collect(Collectors.toList());
        }

        @Override public <S extends Offer> S save(S e) { store.add(e); return e; }
        @Override public Optional<Offer> findById(Long id) {
            return store.stream().filter(o -> o.getId() != null && o.getId().equals(id)).findFirst();
        }
        @Override public <S extends Offer> List<S> saveAll(Iterable<S> es) { return new ArrayList<>(); }
        @Override public boolean existsById(Long id) { return false; }
        @Override public List<Offer> findAll() { return new ArrayList<>(store); }
        @Override public List<Offer> findAllById(Iterable<Long> ids) { return List.of(); }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(Long id) {}
        @Override public void delete(Offer e) {}
        @Override public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override public void deleteAll(Iterable<? extends Offer> es) {}
        @Override public void deleteAll() {}
        @Override public List<Offer> findAll(Sort s) { return List.of(); }
        @Override public Page<Offer> findAll(Pageable p) { return Page.empty(p); }
        @Override public void flush() {}
        @Override public <S extends Offer> S saveAndFlush(S e) { return save(e); }
        @Override public <S extends Offer> List<S> saveAllAndFlush(Iterable<S> es) { return new ArrayList<>(); }
        @Override public void deleteAllInBatch(Iterable<Offer> es) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public Offer getOne(Long id) { throw new UnsupportedOperationException(); }
        @Override public Offer getById(Long id) { throw new UnsupportedOperationException(); }
        @Override public Offer getReferenceById(Long id) { throw new UnsupportedOperationException(); }
        @Override public <S extends Offer> Optional<S> findOne(Example<S> ex) { throw new UnsupportedOperationException(); }
        @Override public <S extends Offer> List<S> findAll(Example<S> ex) { return List.of(); }
        @Override public <S extends Offer> List<S> findAll(Example<S> ex, Sort s) { return List.of(); }
        @Override public <S extends Offer> Page<S> findAll(Example<S> ex, Pageable p) { return Page.empty(p); }
        @Override public <S extends Offer> long count(Example<S> ex) { return 0; }
        @Override public <S extends Offer> boolean exists(Example<S> ex) { return false; }
        @Override public <S extends Offer, R> R findBy(Example<S> ex, Function<FluentQuery.FetchableFluentQuery<S>, R> q) { throw new UnsupportedOperationException(); }
    }

    // ================================================================
    // Stub ListingRepository (reuse the inner stub from ProductServicePropertyTest)
    // ================================================================

    @SuppressWarnings("all")
    static class StubListingRepository extends ProductServicePropertyTest.StubListingRepository {}

    // ================================================================
    // Stub UserRepository (reuse base stub)
    // ================================================================

    @SuppressWarnings("all")
    static class StubUserRepository extends com.revendo.service.StubUserRepository {}

    // ================================================================
    // Stub NotificationRepository
    // ================================================================

    @SuppressWarnings("all")
    static class StubNotificationRepository implements NotificationRepository {
        @Override public List<Notification> findByUserIdOrderByCreatedAtDesc(Long id) { return List.of(); }
        @Override public List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long id) { return List.of(); }
        @Override public long countByUserIdAndIsReadFalse(Long id) { return 0; }
        @Override public <S extends Notification> S save(S e) { return e; }
        @Override public <S extends Notification> List<S> saveAll(Iterable<S> es) { return new ArrayList<>(); }
        @Override public Optional<Notification> findById(Long id) { return Optional.empty(); }
        @Override public boolean existsById(Long id) { return false; }
        @Override public List<Notification> findAll() { return List.of(); }
        @Override public List<Notification> findAllById(Iterable<Long> ids) { return List.of(); }
        @Override public long count() { return 0; }
        @Override public void deleteById(Long id) {}
        @Override public void delete(Notification e) {}
        @Override public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override public void deleteAll(Iterable<? extends Notification> es) {}
        @Override public void deleteAll() {}
        @Override public List<Notification> findAll(Sort s) { return List.of(); }
        @Override public Page<Notification> findAll(Pageable p) { return Page.empty(p); }
        @Override public void flush() {}
        @Override public <S extends Notification> S saveAndFlush(S e) { return e; }
        @Override public <S extends Notification> List<S> saveAllAndFlush(Iterable<S> es) { return new ArrayList<>(); }
        @Override public void deleteAllInBatch(Iterable<Notification> es) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public Notification getOne(Long id) { throw new UnsupportedOperationException(); }
        @Override public Notification getById(Long id) { throw new UnsupportedOperationException(); }
        @Override public Notification getReferenceById(Long id) { throw new UnsupportedOperationException(); }
        @Override public <S extends Notification> Optional<S> findOne(Example<S> ex) { throw new UnsupportedOperationException(); }
        @Override public <S extends Notification> List<S> findAll(Example<S> ex) { return List.of(); }
        @Override public <S extends Notification> List<S> findAll(Example<S> ex, Sort s) { return List.of(); }
        @Override public <S extends Notification> Page<S> findAll(Example<S> ex, Pageable p) { return Page.empty(p); }
        @Override public <S extends Notification> long count(Example<S> ex) { return 0; }
        @Override public <S extends Notification> boolean exists(Example<S> ex) { return false; }
        @Override public <S extends Notification, R> R findBy(Example<S> ex, Function<FluentQuery.FetchableFluentQuery<S>, R> q) { throw new UnsupportedOperationException(); }
    }
}
