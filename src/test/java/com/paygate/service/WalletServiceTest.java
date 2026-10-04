package com.paygate.service;

import com.paygate.dto.response.WalletBalanceResponse;
import com.paygate.exception.InsufficientBalanceException;
import com.paygate.exception.WalletNotFoundException;
import com.paygate.model.entity.AuditLedger;
import com.paygate.model.entity.WalletAccount;
import com.paygate.model.enums.LedgerEntryType;
import com.paygate.repository.AuditLedgerRepository;
import com.paygate.repository.WalletAccountRepository;
import com.paygate.service.impl.WalletServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletAccountRepository walletRepository;

    @Mock
    private AuditLedgerRepository auditLedgerRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    private WalletAccount testWallet;

    @BeforeEach
    void setUp() {
        testWallet = WalletAccount.builder()
                .id(1L)
                .userId("USR_101")
                .balance(new BigDecimal("1000.00"))
                .currency("INR")
                .version(0L)
                .build();
    }

    @Test
    @DisplayName("Should successfully return wallet balance")
    void testGetWalletBalance() {
        when(walletRepository.findByUserId("USR_101")).thenReturn(Optional.of(testWallet));

        WalletBalanceResponse response = walletService.getWalletBalance("USR_101");

        assertNotNull(response);
        assertEquals("USR_101", response.getUserId());
        assertEquals(new BigDecimal("1000.00"), response.getBalance());
    }

    @Test
    @DisplayName("Should throw WalletNotFoundException when wallet does not exist")
    void testGetWalletBalance_NotFound() {
        when(walletRepository.findByUserId("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> walletService.getWalletBalance("UNKNOWN"));
    }

    @Test
    @DisplayName("Should credit wallet and record double-entry audit ledger")
    void testCreditWallet() {
        when(walletRepository.findByUserId("USR_101")).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(WalletAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletAccount updated = walletService.creditWallet("USR_101", new BigDecimal("500.00"), "TXN_999", "Test Credit");

        assertEquals(new BigDecimal("1500.00"), updated.getBalance());
        verify(auditLedgerRepository).save(any(AuditLedger.class));
    }

    @Test
    @DisplayName("Should debit wallet when balance is sufficient")
    void testDebitWallet_Success() {
        when(walletRepository.findByUserId("USR_101")).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(WalletAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletAccount updated = walletService.debitWallet("USR_101", new BigDecimal("400.00"), "TXN_888", "Test Debit");

        assertEquals(new BigDecimal("600.00"), updated.getBalance());
        verify(auditLedgerRepository).save(any(AuditLedger.class));
    }

    @Test
    @DisplayName("Should throw InsufficientBalanceException when debit exceeds balance")
    void testDebitWallet_InsufficientBalance() {
        when(walletRepository.findByUserId("USR_101")).thenReturn(Optional.of(testWallet));

        assertThrows(InsufficientBalanceException.class, () ->
                walletService.debitWallet("USR_101", new BigDecimal("2000.00"), "TXN_FAIL", "Overdraft")
        );
    }
}
