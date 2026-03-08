package com.fintech.ewallet.identity.application.dto;

import java.util.UUID;

/**
 * Response DTO returned after successful registration.
 */
public record RegisterResponse(
                UUID id,
                String phoneNumber,
                String fullName,
                String accountNumber,
                String message) {
}
