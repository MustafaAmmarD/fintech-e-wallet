package com.fintech.ewallet.kyc.application.dto;

import com.fintech.ewallet.identity.domain.KycStatus;

import java.util.UUID;

/**
 * Response after setting a user account back to KYC pending.
 */
public record PendKycAccountResponse(
        UUID userId,
        KycStatus userKycStatus,
        String message) {
}
