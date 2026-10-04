package com.paygate.exception;

public class WalletNotFoundException extends RuntimeException {
    public WalletNotFoundException(String identifier) {
        super("Wallet account not found for identifier: " + identifier);
    }
}
