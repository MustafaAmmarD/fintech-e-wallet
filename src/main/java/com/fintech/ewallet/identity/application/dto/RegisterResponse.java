package com.fintech.ewallet.identity.application.dto;

import java.util.UUID;

/**
 * Response DTO returned after successful registration.
 */
public record RegisterResponse(
        UUID userId,
        String phoneNumber,
        String fullName,
        String referralCode,
        String message) {
}
