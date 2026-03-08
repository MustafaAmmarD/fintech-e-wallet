package com.fintech.ewallet.shared.exception;

public class IdempotencyKeyConflictException extends DomainException {

    public IdempotencyKeyConflictException() {
        super("IDEMPOTENCY_KEY_CONFLICT",
                "Idempotency-Key was already used with a different request payload.");
    }
}
