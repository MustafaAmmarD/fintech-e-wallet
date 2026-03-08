package com.fintech.ewallet.shared.exception;

public class IdempotencyKeyRequiredException extends DomainException {

    public IdempotencyKeyRequiredException() {
        super("IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key header is required for this operation.");
    }
}
