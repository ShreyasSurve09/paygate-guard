package com.paygate.model.entity;

import com.paygate.model.enums.DlqStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Dead Letter Queue (DLQ) Entity.
 * Stores failed webhook events for retry with exponential backoff and manual reconciliation.
 */
@Entity
@Table(name = "failed_webhook_queue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class FailedWebhookQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Idempotency key from the failed webhook request
    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    // Complete raw JSON string to be re-parsed during retries
    @Column(name = "raw_payload", columnDefinition = "TEXT", nullable = false)
    private String rawPayload;

    // How many times this webhook has been retried so far
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    // Max attempts allowed before moving to permanent failure (e.g., 3 retries)
    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private int maxRetries = 3;

    // Stack trace or error message from the last failed attempt
    @Column(name = "last_error_message", length = 1000)
    private String lastErrorMessage;

    // DLQ status: UNRESOLVED, RESOLVED_MANUAL, DISCARDED
    @Enumerated(EnumType.STRING)
    @Column(name = "dlq_status", nullable = false, length = 20)
    private DlqStatus dlqStatus;

    // Timestamp when the next retry should trigger (calculated using exponential backoff)
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.dlqStatus == null) {
            this.dlqStatus = DlqStatus.UNRESOLVED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
