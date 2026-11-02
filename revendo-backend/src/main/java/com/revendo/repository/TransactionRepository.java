package com.revendo.repository;

import com.revendo.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Transaction} entities.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /** Find the single transaction for a given listing (listings are 1-to-1 with transactions). */
    Optional<Transaction> findByListingId(Long listingId);

    /** All purchases or rentals made by a specific buyer. */
    List<Transaction> findByBuyerId(Long buyerId);

    /** All purchases made by a specific buyer. */
    List<Transaction> findByBuyerIdAndTransactionType(
            Long buyerId, Transaction.TransactionType transactionType);

    /** Check whether any transaction exists for a given listing. */
    boolean existsByListingId(Long listingId);
}
