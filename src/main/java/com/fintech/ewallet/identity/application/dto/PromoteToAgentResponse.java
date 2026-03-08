package com.fintech.ewallet.identity.application.dto;

import java.util.UUID;

/**
 * Response for promoting a user to AGENT role.
 */
public record PromoteToAgentResponse(
        UUID userId,
        String fullName,
        String role,
        String message) {
}
