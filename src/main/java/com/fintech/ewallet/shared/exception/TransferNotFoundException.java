package com.fintech.ewallet.shared.exception;

public class TransferNotFoundException extends DomainException {
    public TransferNotFoundException(String message) {
        super("TRANSFER_NOT_FOUND", message);
    }
}
