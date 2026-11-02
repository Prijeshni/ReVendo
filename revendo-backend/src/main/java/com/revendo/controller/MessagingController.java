package com.revendo.controller;

import com.revendo.dto.request.SendMessageRequest;
import com.revendo.dto.request.StartConversationRequest;
import com.revendo.dto.response.ConversationResponse;
import com.revendo.dto.response.MessageResponse;
import com.revendo.service.MessagingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for messaging endpoints.
 *
 * All endpoints require JWT authentication.
 *
 * POST /api/messages/conversations             — start or retrieve conversation
 * GET  /api/messages/conversations             — list all user conversations
 * GET  /api/messages/conversations/:id         — get messages in a conversation
 * POST /api/messages/conversations/:id         — send a message
 * PUT  /api/messages/conversations/:id/read    — mark messages as read
 * GET  /api/messages/unread-count              — total unread message count
 *
 * Requirements: 9.1–9.10
 */
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessagingController {

    private final MessagingService messagingService;

    // ----------------------------------------------------------------
    // POST /api/messages/conversations  — start or retrieve conversation
    // ----------------------------------------------------------------

    /**
     * The buyer calls this to contact the seller of a listing.
     * Returns the existing conversation if one already exists (200),
     * or creates and returns a new one (201 is also acceptable; we use 200
     * for idempotency — the client does not need to distinguish).
     */
    @PostMapping("/conversations")
    public ResponseEntity<ConversationResponse> getOrCreateConversation(
            @Valid @RequestBody StartConversationRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        Long buyerId = extractUserId(principal);
        ConversationResponse conv = messagingService.getOrCreateConversation(
                request.getListingId(), buyerId);
        return ResponseEntity.ok(conv);
    }

    // ----------------------------------------------------------------
    // GET /api/messages/conversations  — list all conversations
    // ----------------------------------------------------------------

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> getConversations(
            @AuthenticationPrincipal UserDetails principal) {

        return ResponseEntity.ok(
                messagingService.getConversations(extractUserId(principal)));
    }

    // ----------------------------------------------------------------
    // GET /api/messages/conversations/:id  — get messages
    // ----------------------------------------------------------------

    /**
     * Returns all messages in the conversation ordered by timestamp ascending.
     * Only participants (buyer or seller) may call this.
     */
    @GetMapping("/conversations/{id}")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {

        Long requesterId = extractUserId(principal);
        return ResponseEntity.ok(messagingService.getMessages(id, requesterId));
    }

    // ----------------------------------------------------------------
    // POST /api/messages/conversations/:id  — send a message
    // ----------------------------------------------------------------

    /**
     * Sends a message into an existing conversation.
     * Body: { "text": "..." }
     * Returns 201 with the saved message.
     */
    @PostMapping("/conversations/{id}")
public ResponseEntity<MessageResponse> sendMessage(
        @PathVariable Long id,
        @Valid @RequestBody SendMessageRequest request,
        @AuthenticationPrincipal UserDetails principal) {

    System.out.println("========== MESSAGE DEBUG ==========");
    System.out.println("Principal username: " +
            (principal != null ? principal.getUsername() : "NULL"));
    System.out.println("Principal authorities: " +
            (principal != null ? principal.getAuthorities() : "NULL"));

    Long senderId = extractUserId(principal);

    System.out.println("Extracted sender ID: " + senderId);

    MessageResponse msg = messagingService.sendMessage(
            id, senderId, request.getText());

    System.out.println("Saved message sender ID: " + msg.getSenderId());
    System.out.println("===================================");

    return ResponseEntity.status(201).body(msg);
}

    // ----------------------------------------------------------------
    // PUT /api/messages/conversations/:id/read  — mark as read
    // ----------------------------------------------------------------

    /**
     * Marks all unread messages from the other participant as read.
     * Returns 200 with a confirmation message.
     */
    @PutMapping("/conversations/{id}/read")
    public ResponseEntity<Map<String, String>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {

        Long readerId = extractUserId(principal);
        messagingService.markAsRead(id, readerId);
        return ResponseEntity.ok(Map.of("message", "Messages marked as read."));
    }

    // ----------------------------------------------------------------
    // GET /api/messages/unread-count  — unread count
    // ----------------------------------------------------------------

    /**
     * Returns the total number of unread messages across all the user's conversations.
     * Response: { "unreadCount": N }
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal UserDetails principal) {

        long count = messagingService.getUnreadCount(extractUserId(principal));
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    // ----------------------------------------------------------------
    // Private helper
    // ----------------------------------------------------------------

    private Long extractUserId(UserDetails principal) {
        return Long.parseLong(principal.getUsername());
    }
}
