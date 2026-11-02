package com.revendo.repository;

import com.revendo.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Conversation} entities.
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /**
     * Look up the unique conversation for a buyer–seller–listing triple.
     * Used by getOrCreateConversation to avoid duplicates (Requirement 9.2/9.3).
     */
    Optional<Conversation> findByListingIdAndBuyerIdAndSellerId(
            Long listingId, Long buyerId, Long sellerId);

    /**
     * All conversations the given user participates in (as buyer or seller),
     * ordered by the most recently active first.
     */
    @Query("""
           SELECT c FROM Conversation c
           WHERE c.buyer.id = :userId OR c.seller.id = :userId
           ORDER BY c.lastMessageAt DESC NULLS LAST, c.createdAt DESC
           """)
    List<Conversation> findByParticipantId(@Param("userId") Long userId);
}
