package com.fintech.ewallet.referral.application.dto;

import com.fintech.ewallet.referral.domain.ReferralStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReferralHistoryResponse(
        UUID referralId,
        UUID refereeUserId,
        String refereeDisplayName,
        ReferralStatus status,
        BigDecimal referrerReward,
        BigDecimal refereeReward,
        Instant rewardedAt,
        Instant createdAt) {
}
