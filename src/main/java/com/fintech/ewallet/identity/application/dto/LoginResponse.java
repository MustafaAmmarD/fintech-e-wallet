package com.fintech.ewallet.identity.application.dto;

import java.util.UUID;

/**
 * Response DTO returned after successful login.
 * Contains JWT tokens and basic user info.
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserInfo user) {
    /**
     * Nested record with basic user info included in login response.
     */
    public record UserInfo(
            UUID id,
            String fullName,
            String phoneNumber,
            String kycStatus) {
    }
}
