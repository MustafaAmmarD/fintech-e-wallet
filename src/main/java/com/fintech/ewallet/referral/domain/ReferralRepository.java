package com.fintech.ewallet.referral.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferralRepository {

    Referral save(Referral referral);

    Optional<Referral> findByRefereeId(UUID refereeId);

    Optional<Referral> findPendingByRefereeId(UUID refereeId);

    List<Referral> findByReferrerIdOrderByCreatedAtDesc(UUID referrerId, int limit);

    long countByReferrerId(UUID referrerId);

    long countByReferrerIdAndStatus(UUID referrerId, ReferralStatus status);

    BigDecimal sumReferrerRewardsByReferrerId(UUID referrerId);
}
