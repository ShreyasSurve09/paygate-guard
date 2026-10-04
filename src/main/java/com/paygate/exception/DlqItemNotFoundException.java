package com.paygate.exception;

public class DlqItemNotFoundException extends RuntimeException {
    public DlqItemNotFoundException(Long id) {
        super("Dead-Letter Queue item not found with ID: " + id);
    }
}
