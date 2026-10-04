package com.paygate.repository;

import com.paygate.model.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for querying payment webhook transactions.
 */
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    /**
     * Primary lookup for Idempotency verification.
     */
    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);

    /**
     * Fast check to see if an idempotency key has already been seen.
     */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * Retrieve all transactions for a user, newest first.
     */
    List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(String userId);
}
