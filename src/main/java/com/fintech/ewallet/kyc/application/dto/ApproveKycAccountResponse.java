package com.fintech.ewallet.kyc.application.dto;

import com.fintech.ewallet.identity.domain.KycStatus;

import java.util.UUID;

/**
 * Response after approving an account's pending KYC documents.
 */
public record ApproveKycAccountResponse(
        UUID userId,
        int approvedDocuments,
        KycStatus userKycStatus,
        String message) {
}
