package com.fintech.ewallet.identity.application.dto;

/**
 * Response DTO after refresh token.
 * Returns new access + refresh tokens.
 */
public record RefreshTokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn) {
}
