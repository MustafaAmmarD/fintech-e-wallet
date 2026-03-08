package com.fintech.ewallet.referral.application.dto;

import java.math.BigDecimal;

public record ReferralStatsResponse(
        long totalReferrals,
        long pendingReferrals,
        long rewardedReferrals,
        BigDecimal totalRewardAmount,
        String rewardCurrency) {
}
