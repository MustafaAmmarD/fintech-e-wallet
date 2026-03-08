package com.fintech.ewallet.referral.application;

import com.fintech.ewallet.referral.application.dto.ReferralStatsResponse;
import com.fintech.ewallet.referral.domain.ReferralRepository;
import com.fintech.ewallet.referral.domain.ReferralStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetReferralStatsUseCase {

    private final ReferralRepository referralRepository;

    public ReferralStatsResponse execute(UUID userId) {
        long totalReferrals = referralRepository.countByReferrerId(userId);
        long rewardedReferrals = referralRepository.countByReferrerIdAndStatus(userId, ReferralStatus.REWARDED);
        long pendingReferrals = referralRepository.countByReferrerIdAndStatus(userId, ReferralStatus.PENDING);
        BigDecimal totalRewardAmount = referralRepository.sumReferrerRewardsByReferrerId(userId);

        return new ReferralStatsResponse(
                totalReferrals,
                pendingReferrals,
                rewardedReferrals,
                totalRewardAmount,
                "YER");
    }
}
