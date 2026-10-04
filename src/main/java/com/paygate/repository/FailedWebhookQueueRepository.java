package com.paygate.repository;

import com.paygate.model.entity.FailedWebhookQueue;
import com.paygate.model.enums.DlqStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Dead Letter Queue (DLQ) entries.
 */
@Repository
public interface FailedWebhookQueueRepository extends JpaRepository<FailedWebhookQueue, Long> {

    /**
     * Finds all webhook events that are unresolved and due for retry.
     */
    List<FailedWebhookQueue> findByDlqStatusAndNextRetryAtBefore(DlqStatus dlqStatus, LocalDateTime currentTime);

    /**
     * Look up failed webhook entry by idempotency key.
     */
    Optional<FailedWebhookQueue> findByIdempotencyKey(String idempotencyKey);
}
