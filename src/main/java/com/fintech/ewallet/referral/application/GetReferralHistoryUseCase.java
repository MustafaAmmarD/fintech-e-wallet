package com.fintech.ewallet.referral.application;

import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.referral.application.dto.ReferralHistoryResponse;
import com.fintech.ewallet.referral.domain.Referral;
import com.fintech.ewallet.referral.domain.ReferralRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetReferralHistoryUseCase {

    private final ReferralRepository referralRepository;
    private final UserRepository userRepository;

    public List<ReferralHistoryResponse> execute(UUID userId, int limit) {
        int clampedLimit = Math.min(Math.max(limit, 1), 50);
        List<Referral> referrals = referralRepository.findByReferrerIdOrderByCreatedAtDesc(userId, clampedLimit);

        return referrals.stream()
                .map(this::toResponse)
                .toList();
    }

    private ReferralHistoryResponse toResponse(Referral referral) {
        String refereeName = userRepository.findById(referral.getRefereeId())
                .map(User::getFullName)
                .orElse("Unknown");

        return new ReferralHistoryResponse(
                referral.getId(),
                referral.getRefereeId(),
                refereeName,
                referral.getStatus(),
                referral.getReferrerReward(),
                referral.getRefereeReward(),
                referral.getRewardedAt(),
                referral.getCreatedAt());
    }
}
