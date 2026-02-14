package com.fintech.ewallet.kyc.application.dto;

import com.fintech.ewallet.identity.domain.KycStatus;
import com.fintech.ewallet.kyc.domain.DocumentType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response containing user's KYC status and uploaded documents.
 */
public record KycStatusResponse(
                KycStatus overallStatus,
                List<DocumentInfo> documents) {
        public record DocumentInfo(
                        UUID id,
                        DocumentType type,
                        String fileName,
                        KycStatus status,
                        String rejectionReason,
                        Instant uploadedAt) {
        }
}
