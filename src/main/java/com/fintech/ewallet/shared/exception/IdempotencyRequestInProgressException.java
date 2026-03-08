package com.fintech.ewallet.shared.exception;

public class IdempotencyRequestInProgressException extends DomainException {

    public IdempotencyRequestInProgressException() {
        super("IDEMPOTENCY_REQUEST_IN_PROGRESS",
                "A request with this Idempotency-Key is already being processed.");
    }
}
