package com.fintech.ewallet.identity.application.dto;

/**
 * Request DTO for token refresh.
 */
public record RefreshTokenRequest(
// Token is extracted from Authorization header, not body
// This is just a marker record for documentation
) {
}
