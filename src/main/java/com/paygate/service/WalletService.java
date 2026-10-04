package com.paygate.service;

import com.paygate.dto.request.WalletTransferRequest;
import com.paygate.dto.response.WalletBalanceResponse;
import com.paygate.model.entity.AuditLedger;
import com.paygate.model.entity.WalletAccount;

import java.math.BigDecimal;
import java.util.List;

public interface WalletService {

    WalletBalanceResponse getWalletBalance(String userId);

    WalletAccount getOrCreateWallet(String userId);

    WalletAccount creditWallet(String userId, BigDecimal amount, String transactionRef, String description);

    WalletAccount debitWallet(String userId, BigDecimal amount, String transactionRef, String description);

    void transferBetweenWallets(WalletTransferRequest transferRequest, String transactionRef);

    List<AuditLedger> getAuditTrail(String userId);
}
