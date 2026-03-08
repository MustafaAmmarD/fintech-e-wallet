package com.fintech.ewallet.kyc.application.dto;

import com.fintech.ewallet.identity.domain.KycStatus;

import java.util.UUID;

/**
 * Response after approving a KYC document.
 */
public record ApproveKycDocumentResponse(
        UUID documentId,
        UUID userId,
        KycStatus userKycStatus,
        String message) {
}
