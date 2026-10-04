package com.paygate.service;

import com.paygate.dto.request.WebhookPaymentRequest;
import com.paygate.dto.response.WebhookProcessResponse;
import com.paygate.model.entity.PaymentTransaction;
import com.paygate.model.entity.WalletAccount;
import com.paygate.model.enums.PaymentStatus;
import com.paygate.repository.PaymentTransactionRepository;
import com.paygate.service.impl.IdempotencyServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private PaymentTransactionRepository transactionRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private BankingClientSimulator bankingSimulator;

    @Mock
    private DlqService dlqService;

    @InjectMocks
    private IdempotencyServiceImpl idempotencyService;

    @Test
    @DisplayName("Should detect duplicate idempotency key and ignore double-crediting")
    void testProcessPaymentWebhook_DuplicateIgnored() {
        String key = "pay_dup_12345";
        WebhookPaymentRequest request = WebhookPaymentRequest.builder()
                .idempotencyKey(key)
                .paymentGatewayId("gtw_999")
                .userId("USR_101")
                .amount(new BigDecimal("500.00"))
                .currency("INR")
                .build();

        PaymentTransaction existingTxn = PaymentTransaction.builder()
                .id(10L)
                .idempotencyKey(key)
                .amount(new BigDecimal("500.00"))
                .status(PaymentStatus.SUCCESS)
                .build();

        WalletAccount wallet = WalletAccount.builder()
                .userId("USR_101")
                .balance(new BigDecimal("1500.00"))
                .build();

        when(transactionRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(existingTxn));
        when(walletService.getOrCreateWallet("USR_101")).thenReturn(wallet);

        WebhookProcessResponse response = idempotencyService.processPaymentWebhook(request);

        assertTrue(response.isDuplicateIgnored());
        assertEquals(PaymentStatus.DUPLICATE_IGNORED, response.getStatus());
        verify(walletService, never()).creditWallet(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should process new webhook successfully and credit wallet")
    void testProcessPaymentWebhook_Success() {
        String key = "pay_new_999";
        WebhookPaymentRequest request = WebhookPaymentRequest.builder()
                .idempotencyKey(key)
                .paymentGatewayId("gtw_555")
                .userId("USR_102")
                .amount(new BigDecimal("750.00"))
                .currency("INR")
                .simulateFailure(false)
                .build();

        WalletAccount wallet = WalletAccount.builder()
                .userId("USR_102")
                .balance(new BigDecimal("1750.00"))
                .build();

        when(transactionRepository.findByIdempotencyKey(key)).thenReturn(Optional.empty());
        when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction txn = invocation.getArgument(0);
            txn.setId(100L);
            return txn;
        });
        when(bankingSimulator.verifyAndClearFunds("gtw_555", false)).thenReturn(true);
        when(walletService.creditWallet(any(), any(), any(), any())).thenReturn(wallet);

        WebhookProcessResponse response = idempotencyService.processPaymentWebhook(request);

        assertFalse(response.isDuplicateIgnored());
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals(new BigDecimal("750.00"), response.getCreditedAmount());
        verify(walletService).creditWallet("USR_102", new BigDecimal("750.00"), key, "Webhook settlement: gtw_555");
    }
}
