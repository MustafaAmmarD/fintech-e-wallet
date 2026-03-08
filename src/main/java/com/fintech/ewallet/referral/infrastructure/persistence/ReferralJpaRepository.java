package com.fintech.ewallet.referral.infrastructure.persistence;

import com.fintech.ewallet.referral.domain.ReferralStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferralJpaRepository extends JpaRepository<ReferralJpaEntity, UUID> {

    Optional<ReferralJpaEntity> findByRefereeId(UUID refereeId);

    Optional<ReferralJpaEntity> findByRefereeIdAndStatus(UUID refereeId, ReferralStatus status);

    List<ReferralJpaEntity> findByReferrerIdOrderByCreatedAtDesc(UUID referrerId, Pageable pageable);

    long countByReferrerId(UUID referrerId);

    long countByReferrerIdAndStatus(UUID referrerId, ReferralStatus status);

    @Query("SELECT COALESCE(SUM(r.referrerReward), 0) FROM ReferralJpaEntity r " +
            "WHERE r.referrerId = :referrerId AND r.status = :status")
    BigDecimal sumReferrerRewardsByReferrerIdAndStatus(
            @Param("referrerId") UUID referrerId,
            @Param("status") ReferralStatus status);
}
