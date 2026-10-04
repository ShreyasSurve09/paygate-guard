package com.paygate.model.enums;

/**
 * Represents the lifecycle state of an incoming payment transaction.
 */
public enum PaymentStatus {
    PENDING,            // Webhook received, processing initiated
    SUCCESS,            // Wallet credited & transaction committed
    FAILED,             // Bank rejected or processing error
    DUPLICATE_IGNORED   // Idempotent hit: already processed earlier, ignored safely
}
