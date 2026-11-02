package com.revendo.repository;

import com.revendo.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Repository for {@link Message} entities.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * All messages in a conversation, ordered by sent time ascending.
     */
    List<Message> findByConversationIdOrderBySentAtAsc(Long conversationId);

    /**
     * Mark all messages sent by the other participant as read.
     * Called when a user opens a conversation (Requirement 9.8).
     *
     * @param conversationId the conversation being opened
     * @param readerId       the user who is reading (NOT the sender we want to mark)
     */
    @Modifying
    @Transactional
    @Query("""
           UPDATE Message m SET m.isRead = true
           WHERE m.conversation.id = :conversationId
             AND m.sender.id <> :readerId
             AND m.isRead = false
           """)
    void markMessagesAsRead(
            @Param("conversationId") Long conversationId,
            @Param("readerId") Long readerId
    );

    /**
     * Count total unread messages for a user across all their conversations
     * (messages sent by the OTHER participant that they haven't read yet).
     */
    @Query("""
           SELECT COUNT(m) FROM Message m
           JOIN m.conversation c
           WHERE (c.buyer.id = :userId OR c.seller.id = :userId)
             AND m.sender.id <> :userId
             AND m.isRead = false
           """)
    long countUnreadByUserId(@Param("userId") Long userId);
}
