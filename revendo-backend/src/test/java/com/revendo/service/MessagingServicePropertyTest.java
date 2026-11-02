package com.revendo.service;

// Feature: re-vendo-marketplace
// Property 13: Conversation uniqueness per buyer-seller-listing triple
// Property 14: Message access restricted to participants

import com.revendo.exception.UnauthorizedException;
import com.revendo.model.*;
import com.revendo.repository.*;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for {@link MessagingService}.
 *
 * Pure unit tests — no Spring context, no database.
 *
 * Property 13 — Validates: Requirements 9.2, 9.3
 * Property 14 — Validates: Requirement 9.7
 */
class MessagingServicePropertyTest {

    // ---------- fixtures ----------

    private static final User SELLER = User.builder()
            .id(1L).firstName("Alice").lastName("Seller")
            .email("seller@test.com").phoneNumber("11111111")
            .passwordHash("h").role(User.Role.ROLE_USER)
            .status(User.Status.Active).build();

    private static final User BUYER = User.builder()
            .id(2L).firstName("Bob").lastName("Buyer")
            .email("buyer@test.com").phoneNumber("22222222")
            .passwordHash("h").role(User.Role.ROLE_USER)
            .status(User.Status.Active).build();

    private static final User OUTSIDER = User.builder()
            .id(3L).firstName("Eve").lastName("Other")
            .email("eve@test.com").phoneNumber("33333333")
            .passwordHash("h").role(User.Role.ROLE_USER)
            .status(User.Status.Active).build();

    private static final Listing LISTING = buildListing(10L, SELLER);

    @BeforeProperty
    void setUp() {}      // each property wires its own service instance

    // ================================================================
    // Property 13 — Conversation uniqueness
    // Validates: Requirements 9.2, 9.3
    // ================================================================

    /**
     * Calling getOrCreateConversation N times for the same triple must result in
     * exactly one Conversation being saved — subsequent calls return the existing one.
     */
    @Property(tries = 50)
    void property13_conversationUniqueness(
            @ForAll("repeatCounts") int n
    ) {
        AtomicInteger saveCount = new AtomicInteger(0);
        List<Conversation> convStore = new ArrayList<>();

        MessagingService svc = buildService(convStore, saveCount, new ArrayList<>());

        // Call N times for the same triple
        for (int i = 0; i < n; i++) {
            svc.getOrCreateConversation(LISTING.getId(), BUYER.getId());
        }

        // save() must have been called exactly once
        assertThat(saveCount.get())
                .as("Conversation must be saved only once for N=" + n + " calls")
                .isEqualTo(1);

        // Only one conversation in the store
        assertThat(convStore)
                .as("Exactly one Conversation must exist")
                .hasSize(1);
    }

    @Provide
    Arbitrary<Integer> repeatCounts() {
        return Arbitraries.integers().between(2, 10);
    }

    // ================================================================
    // Property 14 — Message access restricted to participants
    // Validates: Requirement 9.7
    // ================================================================

    /**
     * A user who is neither buyer nor seller of a conversation must receive
     * UnauthorizedException when trying to read or send messages.
     */
    @Property(tries = 50)
    void property14_messageAccessRestrictedToParticipants(
            @ForAll("outsiderIds") Long outsiderId
    ) {
        // Build a pre-existing conversation
        Conversation conv = buildConversation(5L, LISTING, BUYER, SELLER);
        List<Conversation> store = new ArrayList<>(List.of(conv));

        MessagingService svc = buildService(store, new AtomicInteger(0), new ArrayList<>());

        // getMessages with outsider must fail
        assertThatThrownBy(() -> svc.getMessages(conv.getId(), outsiderId))
                .as("getMessages by outsider must throw UnauthorizedException")
                .isInstanceOf(UnauthorizedException.class);

        // sendMessage with outsider must fail
        assertThatThrownBy(() -> svc.sendMessage(conv.getId(), outsiderId, "hello"))
                .as("sendMessage by outsider must throw UnauthorizedException")
                .isInstanceOf(UnauthorizedException.class);
    }

    @Provide
    Arbitrary<Long> outsiderIds() {
        // Any ID that is not SELLER.id or BUYER.id
        return Arbitraries.longs().between(100L, 9_999L);
    }

    // ================================================================
    // Service builder helpers
    // ================================================================

    private MessagingService buildService(
            List<Conversation> convStore,
            AtomicInteger convSaveCount,
            List<Message> msgStore) {

        ConversationRepository convRepo =
                new StubConversationRepository(convStore, convSaveCount);

        MessageRepository msgRepo = new StubMessageRepository(msgStore);

        ListingRepository listingRepo =
                new ProductServicePropertyTest.StubListingRepository() {
                    @Override
                    public Optional<Listing> findById(Long id) {
                        return LISTING.getId().equals(id)
                                ? Optional.of(LISTING) : Optional.empty();
                    }
                };

        UserRepository userRepo = new StubUserRepository() {
            @Override
            public Optional<User> findById(Long id) {
                if (SELLER.getId().equals(id))   return Optional.of(SELLER);
                if (BUYER.getId().equals(id))    return Optional.of(BUYER);
                if (OUTSIDER.getId().equals(id)) return Optional.of(OUTSIDER);
                return Optional.empty();
            }
        };

        return new MessagingService(convRepo, msgRepo, listingRepo, userRepo);
    }

    // ================================================================
    // Model builders
    // ================================================================

    private static Listing buildListing(Long id, User seller) {
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

    private static Conversation buildConversation(
            Long id, Listing listing, User buyer, User seller) {
        Conversation c = Conversation.builder()
                .id(id).listing(listing).buyer(buyer).seller(seller)
                .build();
        c.setMessages(new ArrayList<>());
        return c;
    }

    // ================================================================
    // Stub ConversationRepository
    // ================================================================

    @SuppressWarnings("all")
    static class StubConversationRepository implements ConversationRepository {

        private final List<Conversation> store;
        private final AtomicInteger      saveCount;
        private long                     nextId = 1L;

        StubConversationRepository(List<Conversation> store, AtomicInteger saveCount) {
            this.store     = store;
            this.saveCount = saveCount;
        }

        @Override
        public Optional<Conversation> findByListingIdAndBuyerIdAndSellerId(
                Long listingId, Long buyerId, Long sellerId) {
            return store.stream()
                    .filter(c -> c.getListing().getId().equals(listingId)
                              && c.getBuyer().getId().equals(buyerId)
                              && c.getSeller().getId().equals(sellerId))
                    .findFirst();
        }

        @Override
        public List<Conversation> findByParticipantId(Long userId) {
            return store.stream()
                    .filter(c -> c.getBuyer().getId().equals(userId)
                              || c.getSeller().getId().equals(userId))
                    .collect(Collectors.toList());
        }

        @Override
        public <S extends Conversation> S save(S e) {
            saveCount.incrementAndGet();
            if (e.getId() == null) {
                // simulate auto-generated ID
                injectId(e, nextId++);
            }
            store.removeIf(c -> c.getId() != null && c.getId().equals(e.getId()));
            store.add(e);
            return e;
        }

        @Override public Optional<Conversation> findById(Long id) {
            return store.stream().filter(c -> c.getId() != null && c.getId().equals(id)).findFirst();
        }
        @Override public <S extends Conversation> List<S> saveAll(Iterable<S> es) { return new ArrayList<>(); }
        @Override public boolean existsById(Long id) { return false; }
        @Override public List<Conversation> findAll() { return new ArrayList<>(store); }
        @Override public List<Conversation> findAllById(Iterable<Long> ids) { return List.of(); }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(Long id) {}
        @Override public void delete(Conversation e) {}
        @Override public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override public void deleteAll(Iterable<? extends Conversation> es) {}
        @Override public void deleteAll() {}
        @Override public List<Conversation> findAll(Sort s) { return List.of(); }
        @Override public Page<Conversation> findAll(Pageable p) { return Page.empty(p); }
        @Override public void flush() {}
        @Override public <S extends Conversation> S saveAndFlush(S e) { return save(e); }
        @Override public <S extends Conversation> List<S> saveAllAndFlush(Iterable<S> es) { return new ArrayList<>(); }
        @Override public void deleteAllInBatch(Iterable<Conversation> es) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public Conversation getOne(Long id) { throw new UnsupportedOperationException(); }
        @Override public Conversation getById(Long id) { throw new UnsupportedOperationException(); }
        @Override public Conversation getReferenceById(Long id) { throw new UnsupportedOperationException(); }
        @Override public <S extends Conversation> Optional<S> findOne(Example<S> ex) { throw new UnsupportedOperationException(); }
        @Override public <S extends Conversation> List<S> findAll(Example<S> ex) { return List.of(); }
        @Override public <S extends Conversation> List<S> findAll(Example<S> ex, Sort s) { return List.of(); }
        @Override public <S extends Conversation> Page<S> findAll(Example<S> ex, Pageable p) { return Page.empty(p); }
        @Override public <S extends Conversation> long count(Example<S> ex) { return 0; }
        @Override public <S extends Conversation> boolean exists(Example<S> ex) { return false; }
        @Override public <S extends Conversation, R> R findBy(Example<S> ex, Function<FluentQuery.FetchableFluentQuery<S>, R> q) { throw new UnsupportedOperationException(); }

        private static void injectId(Object target, Long id) {
            try {
                var f = target.getClass().getDeclaredField("id");
                f.setAccessible(true);
                f.set(target, id);
            } catch (Exception ignored) {}
        }
    }

    // ================================================================
    // Stub MessageRepository
    // ================================================================

    @SuppressWarnings("all")
    static class StubMessageRepository implements MessageRepository {

        private final List<Message> store;

        StubMessageRepository(List<Message> store) {
            this.store = store;
        }

        @Override
        public List<Message> findByConversationIdOrderBySentAtAsc(Long convId) {
            return store.stream()
                    .filter(m -> m.getConversation().getId().equals(convId))
                    .sorted(Comparator.comparing(m -> m.getSentAt() != null
                            ? m.getSentAt()
                            : java.time.LocalDateTime.MIN))
                    .collect(Collectors.toList());
        }

        @Override
        public void markMessagesAsRead(Long conversationId, Long readerId) {
            store.stream()
                    .filter(m -> m.getConversation().getId().equals(conversationId)
                              && !m.getSender().getId().equals(readerId))
                    .forEach(m -> {
                        try {
                            var f = m.getClass().getDeclaredField("isRead");
                            f.setAccessible(true);
                            f.set(m, true);
                        } catch (Exception ignored) {}
                    });
        }

        @Override
        public long countUnreadByUserId(Long userId) {
            return store.stream()
                    .filter(m -> !m.getSender().getId().equals(userId) && !m.isRead())
                    .count();
        }

        @Override public <S extends Message> S save(S e) { store.add(e); return e; }
        @Override public Optional<Message> findById(Long id) { return Optional.empty(); }
        @Override public <S extends Message> List<S> saveAll(Iterable<S> es) { return new ArrayList<>(); }
        @Override public boolean existsById(Long id) { return false; }
        @Override public List<Message> findAll() { return new ArrayList<>(store); }
        @Override public List<Message> findAllById(Iterable<Long> ids) { return List.of(); }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(Long id) {}
        @Override public void delete(Message e) {}
        @Override public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override public void deleteAll(Iterable<? extends Message> es) {}
        @Override public void deleteAll() {}
        @Override public List<Message> findAll(Sort s) { return List.of(); }
        @Override public Page<Message> findAll(Pageable p) { return Page.empty(p); }
        @Override public void flush() {}
        @Override public <S extends Message> S saveAndFlush(S e) { return e; }
        @Override public <S extends Message> List<S> saveAllAndFlush(Iterable<S> es) { return new ArrayList<>(); }
        @Override public void deleteAllInBatch(Iterable<Message> es) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public Message getOne(Long id) { throw new UnsupportedOperationException(); }
        @Override public Message getById(Long id) { throw new UnsupportedOperationException(); }
        @Override public Message getReferenceById(Long id) { throw new UnsupportedOperationException(); }
        @Override public <S extends Message> Optional<S> findOne(Example<S> ex) { throw new UnsupportedOperationException(); }
        @Override public <S extends Message> List<S> findAll(Example<S> ex) { return List.of(); }
        @Override public <S extends Message> List<S> findAll(Example<S> ex, Sort s) { return List.of(); }
        @Override public <S extends Message> Page<S> findAll(Example<S> ex, Pageable p) { return Page.empty(p); }
        @Override public <S extends Message> long count(Example<S> ex) { return 0; }
        @Override public <S extends Message> boolean exists(Example<S> ex) { return false; }
        @Override public <S extends Message, R> R findBy(Example<S> ex, Function<FluentQuery.FetchableFluentQuery<S>, R> q) { throw new UnsupportedOperationException(); }
    }

    // Reuse base stubs from existing tests
    static class StubUserRepository extends com.revendo.service.StubUserRepository {}
}
