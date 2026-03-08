package com.fintech.ewallet.referral.infrastructure.persistence;

import com.fintech.ewallet.referral.domain.Referral;
import org.springframework.stereotype.Component;

@Component
public class ReferralMapper {

    public ReferralJpaEntity toEntity(Referral domain) {
        ReferralJpaEntity entity = new ReferralJpaEntity();
        entity.setId(domain.getId());
        entity.setReferrerId(domain.getReferrerId());
        entity.setRefereeId(domain.getRefereeId());
        entity.setReferralCode(domain.getReferralCode());
        entity.setStatus(domain.getStatus());
        entity.setReferrerReward(domain.getReferrerReward());
        entity.setRefereeReward(domain.getRefereeReward());
        entity.setRewardedAt(domain.getRewardedAt());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    public Referral toDomain(ReferralJpaEntity entity) {
        Referral domain = new Referral();
        domain.setId(entity.getId());
        domain.setReferrerId(entity.getReferrerId());
        domain.setRefereeId(entity.getRefereeId());
        domain.setReferralCode(entity.getReferralCode());
        domain.setStatus(entity.getStatus());
        domain.setReferrerReward(entity.getReferrerReward());
        domain.setRefereeReward(entity.getRefereeReward());
        domain.setRewardedAt(entity.getRewardedAt());
        domain.setCreatedAt(entity.getCreatedAt());
        return domain;
    }
}
