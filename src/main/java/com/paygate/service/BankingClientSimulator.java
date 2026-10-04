package com.paygate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * Simulates third-party bank / payment network downstream responses.
 * Demonstrates how real-world distributed banking systems face network timeouts and 503 drops.
 */
@Service
@Slf4j
public class BankingClientSimulator {

    private final Random random = new Random();

    /**
     * Simulates clearing funds with the issuing bank.
     * Throws an exception if simulated failure is requested or if simulated random timeout occurs.
     */
    public boolean verifyAndClearFunds(String gatewayTxnId, boolean forceFailure) {
        log.info("Contacting downstream banking gateway for settlement: {}", gatewayTxnId);

        if (forceFailure) {
            log.warn("Downstream bank gateway simulated HTTP 503 Service Unavailable for txn: {}", gatewayTxnId);
            throw new RuntimeException("Bank gateway timeout: 503 Gateway Timeout for txn: " + gatewayTxnId);
        }

        // Simulates fast 10ms network round-trip
        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        log.info("Bank gateway confirmed fund settlement for txn: {}", gatewayTxnId);
        return true;
    }
}
