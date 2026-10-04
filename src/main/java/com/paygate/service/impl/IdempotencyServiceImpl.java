package com.paygate.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paygate.dto.request.WebhookPaymentRequest;
import com.paygate.dto.response.WebhookProcessResponse;
import com.paygate.model.entity.PaymentTransaction;
import com.paygate.model.entity.WalletAccount;
import com.paygate.model.enums.PaymentStatus;
import com.paygate.repository.PaymentTransactionRepository;
import com.paygate.service.BankingClientSimulator;
import com.paygate.service.DlqService;
import com.paygate.service.IdempotencyService;
import com.paygate.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyServiceImpl implements IdempotencyService {

    private final PaymentTransactionRepository transactionRepository;
    private final WalletService walletService;
    private final BankingClientSimulator bankingSimulator;
    private final DlqService dlqService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WebhookProcessResponse processPaymentWebhook(WebhookPaymentRequest request) {
        String key = request.getIdempotencyKey();
        log.info("Processing incoming webhook with Idempotency Key: {}", key);

        // 1. Check if this webhook was already processed (Idempotency Guard)
        Optional<PaymentTransaction> existingTxn = transactionRepository.findByIdempotencyKey(key);
        if (existingTxn.isPresent()) {
            PaymentTransaction txn = existingTxn.get();
            if (txn.getStatus() == PaymentStatus.SUCCESS) {
                log.warn("IDEMPOTENCY HIT: Webhook with key {} already processed on {}. Ignoring duplicate event.", key, txn.getCreatedAt());
                WalletAccount wallet = walletService.getOrCreateWallet(request.getUserId());
                return WebhookProcessResponse.builder()
                        .idempotencyKey(key)
                        .status(PaymentStatus.DUPLICATE_IGNORED)
                        .message("Duplicate webhook ignored. Transaction was already settled successfully.")
                        .transactionId(txn.getId())
                        .creditedAmount(txn.getAmount())
                        .currentWalletBalance(wallet.getBalance())
                        .duplicateIgnored(true)
                        .processedAt(LocalDateTime.now())
                        .build();
            }
        }

        String rawJson = serializePayload(request);

        // 2. Persist transaction as PENDING
        PaymentTransaction transaction = existingTxn.orElseGet(() ->
                PaymentTransaction.builder()
                        .idempotencyKey(key)
                        .gatewayTxnId(request.getPaymentGatewayId())
                        .userId(request.getUserId())
                        .amount(request.getAmount())
                        .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                        .status(PaymentStatus.PENDING)
                        .rawPayload(rawJson)
                        .build()
        );
        transaction = transactionRepository.save(transaction);

        // 3. Attempt clearance through downstream Banking Gateway
        try {
            bankingSimulator.verifyAndClearFunds(request.getPaymentGatewayId(), request.isSimulateFailure());

            // 4. Credit wallet and create audit ledger
            WalletAccount updatedWallet = walletService.creditWallet(
                    request.getUserId(),
                    request.getAmount(),
                    key,
                    "Webhook settlement: " + request.getPaymentGatewayId()
            );

            // 5. Update transaction to SUCCESS
            transaction.setStatus(PaymentStatus.SUCCESS);
            transaction.setErrorMessage(null);
            transactionRepository.save(transaction);

            log.info("Payment settlement completed successfully for txn ID: {}", transaction.getId());

            return WebhookProcessResponse.builder()
                    .idempotencyKey(key)
                    .status(PaymentStatus.SUCCESS)
                    .message("Payment webhook successfully processed and wallet credited.")
                    .transactionId(transaction.getId())
                    .creditedAmount(request.getAmount())
                    .currentWalletBalance(updatedWallet.getBalance())
                    .duplicateIgnored(false)
                    .processedAt(LocalDateTime.now())
                    .build();

        } catch (Exception ex) {
            log.error("Downstream settlement failed for webhook key {}. Routing to DLQ. Error: {}", key, ex.getMessage());

            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setErrorMessage(ex.getMessage());
            transactionRepository.save(transaction);

            // Route to DLQ
            dlqService.recordFailure(key, rawJson, ex.getMessage());

            WalletAccount wallet = walletService.getOrCreateWallet(request.getUserId());

            return WebhookProcessResponse.builder()
                    .idempotencyKey(key)
                    .status(PaymentStatus.FAILED)
                    .message("Banking settlement failed. Webhook routed to Dead Letter Queue (DLQ) for retry.")
                    .transactionId(transaction.getId())
                    .creditedAmount(request.getAmount())
                    .currentWalletBalance(wallet.getBalance())
                    .duplicateIgnored(false)
                    .processedAt(LocalDateTime.now())
                    .build();
        }
    }

    private String serializePayload(WebhookPaymentRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            return "{ \"idempotencyKey\": \"" + request.getIdempotencyKey() + "\" }";
        }
    }
}
