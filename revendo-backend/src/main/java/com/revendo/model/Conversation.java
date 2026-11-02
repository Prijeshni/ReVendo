package com.revendo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity representing the `conversations` table.
 *
 * Unique constraint on (listing_id, buyer_id, seller_id) ensures
 * exactly one conversation thread per buyer–seller–listing triple.
 */
@Entity
@Table(
    name = "conversations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_conversations_triple",
            columnNames = {"listing_id", "buyer_id", "seller_id"}
        )
    },
    indexes = {
        @Index(name = "idx_conversations_buyer_id",  columnList = "buyer_id"),
        @Index(name = "idx_conversations_seller_id", columnList = "seller_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Updated every time a new message is sent in this conversation. */
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    /** Messages in this conversation (lazy — loaded on demand). */
    @OneToMany(
        mappedBy = "conversation",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<Message> messages = new ArrayList<>();

    // ----- Lifecycle -----

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
