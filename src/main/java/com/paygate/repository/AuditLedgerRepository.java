package com.paygate.repository;

import com.paygate.model.entity.AuditLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for managing and querying immutable Audit Ledger records.
 */
@Repository
public interface AuditLedgerRepository extends JpaRepository<AuditLedger, Long> {

    /**
     * Retrieve the complete audit passbook trail for a given wallet ID,
     * ordered newest first.
     */
    List<AuditLedger> findByWalletIdOrderByCreatedAtDesc(Long walletId);

    /**
     * Look up audit records associated with a specific idempotency or gateway transaction reference.
     */
    List<AuditLedger> findByTransactionRef(String transactionRef);
}
