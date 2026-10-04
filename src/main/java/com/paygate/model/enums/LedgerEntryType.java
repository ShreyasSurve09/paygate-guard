package com.paygate.model.enums;

/**
 * Represents the type of double-entry financial ledger entry.
 */
public enum LedgerEntryType {
    CREDIT,  // Adding funds to customer/merchant wallet
    DEBIT    // Deducting funds from customer/merchant wallet
}
