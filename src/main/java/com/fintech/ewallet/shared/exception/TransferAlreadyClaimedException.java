package com.fintech.ewallet.shared.exception;

public class TransferAlreadyClaimedException extends DomainException {
    public TransferAlreadyClaimedException(String message) {
        super("TRANSFER_ALREADY_CLAIMED", message);
    }
}
