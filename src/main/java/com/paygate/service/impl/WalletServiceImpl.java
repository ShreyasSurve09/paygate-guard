package com.paygate.service.impl;

import com.paygate.dto.request.WalletTransferRequest;
import com.paygate.dto.response.WalletBalanceResponse;
import com.paygate.exception.InsufficientBalanceException;
import com.paygate.exception.WalletNotFoundException;
import com.paygate.model.entity.AuditLedger;
import com.paygate.model.entity.WalletAccount;
import com.paygate.model.enums.LedgerEntryType;
import com.paygate.repository.AuditLedgerRepository;
import com.paygate.repository.WalletAccountRepository;
import com.paygate.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletAccountRepository walletRepository;
    private final AuditLedgerRepository auditLedgerRepository;

    @Override
    @Transactional(readOnly = true)
    public WalletBalanceResponse getWalletBalance(String userId) {
        WalletAccount wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));

        return WalletBalanceResponse.builder()
                .walletId(wallet.getId())
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .version(wallet.getVersion())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public WalletAccount getOrCreateWallet(String userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Creating fresh wallet for user: {}", userId);
                    WalletAccount newWallet = WalletAccount.builder()
                            .userId(userId)
                            .balance(BigDecimal.ZERO)
                            .currency("INR")
                            .build();
                    return walletRepository.save(newWallet);
                });
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WalletAccount creditWallet(String userId, BigDecimal amount, String transactionRef, String description) {
        WalletAccount wallet = getOrCreateWallet(userId);

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);
        wallet.setBalance(balanceAfter);

        WalletAccount updatedWallet = walletRepository.save(wallet);

        // Record double-entry audit ledger entry
        AuditLedger ledger = AuditLedger.builder()
                .walletId(wallet.getId())
                .transactionRef(transactionRef)
                .amount(amount)
                .entryType(LedgerEntryType.CREDIT)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .description(description != null ? description : "Wallet Credit")
                .build();

        auditLedgerRepository.save(ledger);
        log.info("Successfully credited {} to user {}. New balance: {}", amount, userId, balanceAfter);
        return updatedWallet;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WalletAccount debitWallet(String userId, BigDecimal amount, String transactionRef, String description) {
        WalletAccount wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient funds. Available: " + wallet.getBalance() + ", Requested: " + amount);
        }

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.subtract(amount);
        wallet.setBalance(balanceAfter);

        WalletAccount updatedWallet = walletRepository.save(wallet);

        // Record double-entry audit ledger entry
        AuditLedger ledger = AuditLedger.builder()
                .walletId(wallet.getId())
                .transactionRef(transactionRef)
                .amount(amount)
                .entryType(LedgerEntryType.DEBIT)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .description(description != null ? description : "Wallet Debit")
                .build();

        auditLedgerRepository.save(ledger);
        log.info("Successfully debited {} from user {}. New balance: {}", amount, userId, balanceAfter);
        return updatedWallet;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void transferBetweenWallets(WalletTransferRequest request, String transactionRef) {
        debitWallet(request.getSourceUserId(), request.getAmount(), transactionRef, "Transfer to " + request.getTargetUserId());
        creditWallet(request.getTargetUserId(), request.getAmount(), transactionRef, "Transfer from " + request.getSourceUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLedger> getAuditTrail(String userId) {
        WalletAccount wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));
        return auditLedgerRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());
    }
}
