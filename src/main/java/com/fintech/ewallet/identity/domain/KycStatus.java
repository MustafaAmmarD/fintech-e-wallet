package com.fintech.ewallet.identity.domain;

/**
 * KYC verification status for a user account.
 */
public enum KycStatus {
    NONE, // Not submitted
    PENDING, // Submitted, awaiting review
    VERIFIED, // Approved by admin
    REJECTED // Rejected by admin
}
