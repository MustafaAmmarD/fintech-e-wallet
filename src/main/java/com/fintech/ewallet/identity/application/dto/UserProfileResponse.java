package com.fintech.ewallet.identity.application.dto;

import com.fintech.ewallet.identity.domain.AccountStatus;
import com.fintech.ewallet.identity.domain.KycStatus;
import com.fintech.ewallet.identity.domain.UserRole;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for the authenticated user's own profile.
 */
public record UserProfileResponse(
        UUID id,
        String phoneNumber,
        String fullName,
        String englishFullName,
        String gender,
        String dateOfBirth,
        String idNumber,
        String maritalStatus,
        String email,
        String language,
        String accountNumber,
        String referralCode,
        boolean showFullName,
        AccountStatus accountStatus,
        KycStatus kycStatus,
        UserRole role,
        Instant createdAt
) {}
