package com.paygate.controller;

import com.paygate.dto.request.WalletTransferRequest;
import com.paygate.dto.response.WalletBalanceResponse;
import com.paygate.model.entity.AuditLedger;
import com.paygate.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallet Operations", description = "Digital wallet balance checks, P2P transfers, and double-entry audit ledgers")
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/{userId}/balance")
    @Operation(summary = "Get wallet balance", description = "Fetch real-time wallet balance and optimistic lock version")
    public ResponseEntity<WalletBalanceResponse> getBalance(@PathVariable String userId) {
        return ResponseEntity.ok(walletService.getWalletBalance(userId));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Peer-to-peer wallet transfer", description = "Atomically debits source wallet and credits destination wallet with audit records")
    public ResponseEntity<String> transferFunds(@Valid @RequestBody WalletTransferRequest request) {
        String transactionRef = "TRF_" + UUID.randomUUID().toString().substring(0, 8);
        walletService.transferBetweenWallets(request, transactionRef);
        return ResponseEntity.ok("Transfer completed successfully with reference: " + transactionRef);
    }

    @GetMapping("/{userId}/audit-ledger")
    @Operation(summary = "Get immutable audit passbook", description = "Returns chronological credit/debit audit trail for compliance")
    public ResponseEntity<List<AuditLedger>> getAuditLedger(@PathVariable String userId) {
        return ResponseEntity.ok(walletService.getAuditTrail(userId));
    }
}
