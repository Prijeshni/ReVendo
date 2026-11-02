package com.revendo.service;

import com.revendo.dto.response.ConversationResponse;
import com.revendo.dto.response.MessageResponse;
import com.revendo.exception.ResourceNotFoundException;
import com.revendo.exception.UnauthorizedException;
import com.revendo.exception.ValidationException;
import com.revendo.model.Conversation;
import com.revendo.model.Listing;
import com.revendo.model.Message;
import com.revendo.model.User;
import com.revendo.repository.ConversationRepository;
import com.revendo.repository.ListingRepository;
import com.revendo.repository.MessageRepository;
import com.revendo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for the messaging system.
 *
 * Task 8.1 methods:
 *   getOrCreateConversation — find or create the unique conversation for a triple
 *   getConversations        — all conversations the user participates in
 *   getMessages             — all messages in a conversation (participants only)
 *   sendMessage             — add a new message (participants only)
 *   markAsRead              — mark all received messages in a conversation as read
 *   getUnreadCount          — total unread messages across all the user's conversations
 *
 * Requirements: 9.1–9.9
 */
@Service
@RequiredArgsConstructor
public class MessagingService {

    private static final int LAST_MESSAGE_PREVIEW_LENGTH = 100;

    private final ConversationRepository conversationRepository;
    private final MessageRepository      messageRepository;
    private final ListingRepository      listingRepository;
    private final UserRepository         userRepository;

    // ----------------------------------------------------------------
    // getOrCreateConversation  (Requirements 9.1, 9.2, 9.3)
    // ----------------------------------------------------------------

    /**
     * Returns the existing conversation for the (listing, buyer, seller) triple,
     * or creates one if it does not exist yet.
     *
     * The caller is always the buyer — the seller is derived from the listing.
     *
     * @param listingId the listing the buyer wants to discuss
     * @param buyerId   the authenticated buyer's ID (from JWT)
     * @return the conversation summary
     * @throws ResourceNotFoundException if the listing is not found
     * @throws UnauthorizedException     if the buyer is the seller
     */
    @Transactional
    public ConversationResponse getOrCreateConversation(Long listingId, Long buyerId) {

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));

        Long sellerId = listing.getSeller().getId();

        // Buyer must not be the seller
        if (buyerId.equals(sellerId)) {
            throw new UnauthorizedException(
                    "You cannot start a conversation about your own listing.");
        }

        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", buyerId));

        // Return existing conversation or create a new one (Requirement 9.2/9.3)
        Conversation conv = conversationRepository
                .findByListingIdAndBuyerIdAndSellerId(listingId, buyerId, sellerId)
                .orElseGet(() -> {
                    Conversation newConv = Conversation.builder()
                            .listing(listing)
                            .buyer(buyer)
                            .seller(listing.getSeller())
                            .build();
                    return conversationRepository.save(newConv);
                });

        return toConversationResponse(conv);
    }

    // ----------------------------------------------------------------
    // getConversations  (Requirement 9.5)
    // ----------------------------------------------------------------

    /**
     * All conversations for the authenticated user (as buyer or seller),
     * ordered by most recently active first.
     *
     * @param userId the authenticated user's ID (from JWT)
     * @return list of conversation summaries with truncated last message
     */
    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(Long userId) {
        return conversationRepository.findByParticipantId(userId)
                .stream()
                .map(this::toConversationResponse)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    // getMessages  (Requirements 9.6, 9.7)
    // ----------------------------------------------------------------

    /**
     * Returns all messages in a conversation ordered by sent time ascending.
     * Only participants (buyer or seller) may call this.
     *
     * @param conversationId the conversation to read
     * @param requesterId    the authenticated user's ID (from JWT)
     * @return list of messages in chronological order
     * @throws ResourceNotFoundException if the conversation is not found
     * @throws UnauthorizedException     if the requester is not a participant
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long conversationId, Long requesterId) {

        Conversation conv = loadConversation(conversationId);
        assertIsParticipant(requesterId, conv);

        return messageRepository
                .findByConversationIdOrderBySentAtAsc(conversationId)
                .stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    // sendMessage  (Requirements 9.4, 9.10)
    // ----------------------------------------------------------------

    /**
     * Saves a new message in a conversation and updates lastMessageAt.
     * Only participants may send messages.
     *
     * @param conversationId the target conversation
     * @param senderId       the authenticated sender's ID (from JWT)
     * @param text           message body (1–2000 characters)
     * @return the persisted message as a response DTO
     */
    @Transactional
    public MessageResponse sendMessage(Long conversationId, Long senderId, String text) {

        if (text == null || text.isBlank()) {
            throw new ValidationException("Message text must not be blank.");
        }

        Conversation conv = loadConversation(conversationId);
        assertIsParticipant(senderId, conv);

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", senderId));

        Message msg = Message.builder()
                .conversation(conv)
                .sender(sender)
                .text(text)
                .isRead(false)
                .build();

        Message saved = messageRepository.save(msg);

        // Update lastMessageAt on the conversation (Requirement 9.4)
        conv.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conv);

        return toMessageResponse(saved);
    }

    // ----------------------------------------------------------------
    // markAsRead  (Requirement 9.8)
    // ----------------------------------------------------------------

    /**
     * Marks all unread messages sent by the OTHER participant as read.
     * Called when the user opens a conversation.
     *
     * @param conversationId the conversation being opened
     * @param readerId       the authenticated user's ID (from JWT)
     */
    @Transactional
    public void markAsRead(Long conversationId, Long readerId) {

        Conversation conv = loadConversation(conversationId);
        assertIsParticipant(readerId, conv);

        messageRepository.markMessagesAsRead(conversationId, readerId);
    }

    // ----------------------------------------------------------------
    // getUnreadCount  (Requirement 9.9)
    // ----------------------------------------------------------------

    /**
     * Total unread messages the user has not yet read across all their conversations.
     *
     * @param userId the authenticated user's ID (from JWT)
     * @return unread message count
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return messageRepository.countUnreadByUserId(userId);
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    private Conversation loadConversation(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Conversation", conversationId));
    }

    /**
     * Throws {@link UnauthorizedException} if the user is neither buyer nor seller
     * of this conversation (Requirement 9.7).
     */
    private void assertIsParticipant(Long userId, Conversation conv) {
        boolean isBuyer  = conv.getBuyer().getId().equals(userId);
        boolean isSeller = conv.getSeller().getId().equals(userId);
        if (!isBuyer && !isSeller) {
            throw new UnauthorizedException(
                    "You are not a participant in this conversation.");
        }
    }

    private ConversationResponse toConversationResponse(Conversation c) {
        // Build last-message preview (Requirement 9.5)
        List<Message> msgs = messageRepository
                .findByConversationIdOrderBySentAtAsc(c.getId());

        String preview = null;
        if (!msgs.isEmpty()) {
            String lastText = msgs.get(msgs.size() - 1).getText();
            preview = lastText.length() > LAST_MESSAGE_PREVIEW_LENGTH
                    ? lastText.substring(0, LAST_MESSAGE_PREVIEW_LENGTH)
                    : lastText;
        }

        String buyerName  = c.getBuyer().getFirstName()  + " " + c.getBuyer().getLastName();
        String sellerName = c.getSeller().getFirstName() + " " + c.getSeller().getLastName();

        return ConversationResponse.builder()
                .conversationId(c.getId())
                .listingId(c.getListing().getId())
                .listingTitle(c.getListing().getTitle())
                .buyerId(c.getBuyer().getId())
                .buyerDisplayName(buyerName)
                .sellerId(c.getSeller().getId())
                .sellerDisplayName(sellerName)
                .lastMessagePreview(preview)
                .createdAt(c.getCreatedAt())
                .lastMessageAt(c.getLastMessageAt())
                .build();
    }

    private MessageResponse toMessageResponse(Message m) {
        String senderName = m.getSender().getFirstName()
                + " " + m.getSender().getLastName();
        return MessageResponse.builder()
                .messageId(m.getId())
                .conversationId(m.getConversation().getId())
                .senderId(m.getSender().getId())
                .senderDisplayName(senderName)
                .text(m.getText())
                .isRead(m.isRead())
                .sentAt(m.getSentAt())
                .build();
    }
}
