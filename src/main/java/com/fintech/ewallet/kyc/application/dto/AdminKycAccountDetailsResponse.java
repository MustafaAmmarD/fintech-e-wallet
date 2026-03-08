package com.fintech.ewallet.kyc.application.dto;

import com.fintech.ewallet.identity.domain.AccountStatus;
import com.fintech.ewallet.identity.domain.KycStatus;
import com.fintech.ewallet.kyc.domain.DocumentType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Admin detailed view for one user account and all KYC documents.
 */
public record AdminKycAccountDetailsResponse(
        UUID userId,
        String phoneNumber,
        String fullName,
        String email,
        KycStatus kycStatus,
        AccountStatus accountStatus,
        Instant createdAt,
        List<DocumentInfo> documents) {

    public record DocumentInfo(
            UUID documentId,
            DocumentType documentType,
            String fileName,
            String mimeType,
            Long fileSize,
            KycStatus status,
            String rejectionReason,
            Instant uploadedAt,
            Instant reviewedAt,
            UUID reviewedBy) {
    }
}
