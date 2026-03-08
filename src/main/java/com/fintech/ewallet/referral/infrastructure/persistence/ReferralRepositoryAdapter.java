package com.fintech.ewallet.referral.infrastructure.persistence;

import com.fintech.ewallet.referral.domain.Referral;
import com.fintech.ewallet.referral.domain.ReferralRepository;
import com.fintech.ewallet.referral.domain.ReferralStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReferralRepositoryAdapter implements ReferralRepository {

    private final ReferralJpaRepository referralJpaRepository;
    private final ReferralMapper referralMapper;

    @Override
    public Referral save(Referral referral) {
        ReferralJpaEntity saved = referralJpaRepository.save(referralMapper.toEntity(referral));
        return referralMapper.toDomain(saved);
    }

    @Override
    public Optional<Referral> findByRefereeId(UUID refereeId) {
        return referralJpaRepository.findByRefereeId(refereeId).map(referralMapper::toDomain);
    }

    @Override
    public Optional<Referral> findPendingByRefereeId(UUID refereeId) {
        return referralJpaRepository.findByRefereeIdAndStatus(refereeId, ReferralStatus.PENDING)
                .map(referralMapper::toDomain);
    }

    @Override
    public List<Referral> findByReferrerIdOrderByCreatedAtDesc(UUID referrerId, int limit) {
        return referralJpaRepository.findByReferrerIdOrderByCreatedAtDesc(referrerId, PageRequest.of(0, limit))
                .stream()
                .map(referralMapper::toDomain)
                .toList();
    }

    @Override
    public long countByReferrerId(UUID referrerId) {
        return referralJpaRepository.countByReferrerId(referrerId);
    }

    @Override
    public long countByReferrerIdAndStatus(UUID referrerId, ReferralStatus status) {
        return referralJpaRepository.countByReferrerIdAndStatus(referrerId, status);
    }

    @Override
    public BigDecimal sumReferrerRewardsByReferrerId(UUID referrerId) {
        return referralJpaRepository.sumReferrerRewardsByReferrerIdAndStatus(referrerId, ReferralStatus.REWARDED);
    }
}
