package com.paygate.model.enums;

/**
 * Represents the resolution state of a Dead-Letter Queue (DLQ) item.
 */
public enum DlqStatus {
    UNRESOLVED,        // Permanent failure, awaiting human/admin action
    RESOLVED_MANUAL,   // Manually re-triggered and reconciled by admin
    DISCARDED          // Invalid payload or fraud, discarded safely
}
