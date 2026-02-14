package com.fintech.ewallet.kyc.application.dto;

import com.fintech.ewallet.identity.domain.KycStatus;
import com.fintech.ewallet.kyc.domain.DocumentType;

import java.time.Instant;
import java.util.UUID;

/**
 * Response after uploading a KYC document.
 */
public record UploadDocumentResponse(
                UUID documentId,
                DocumentType documentType,
                String fileName,
                KycStatus status,
                Instant uploadedAt) {
}
