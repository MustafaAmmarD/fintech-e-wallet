package com.fintech.ewallet.kyc.application.dto;

import com.fintech.ewallet.identity.domain.AccountStatus;
import com.fintech.ewallet.identity.domain.KycStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Admin summary view for a user account in KYC queue/list pages.
 */
public record AdminKycAccountSummaryResponse(
        UUID userId,
        String phoneNumber,
        String fullName,
        String email,
        KycStatus kycStatus,
        AccountStatus accountStatus,
        long totalDocuments,
        long pendingDocuments,
        Instant createdAt) {
}
