package com.fintech.ewallet.shared.idempotency;

public record IdempotencyStoredResponse(
        int statusCode,
        String contentType,
        byte[] responseBody,
        String requestHash) {
}
