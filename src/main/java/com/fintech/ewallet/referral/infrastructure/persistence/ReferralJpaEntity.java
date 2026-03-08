package com.fintech.ewallet.referral.infrastructure.persistence;

import com.fintech.ewallet.referral.domain.ReferralStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "referrals")
@Getter
@Setter
public class ReferralJpaEntity {

    @Id
    private UUID id;

    @Column(name = "referrer_id", nullable = false)
    private UUID referrerId;

    @Column(name = "referee_id", nullable = false)
    private UUID refereeId;

    @Column(name = "referral_code", nullable = false, length = 20)
    private String referralCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReferralStatus status;

    @Column(name = "referrer_reward", precision = 19, scale = 4)
    private BigDecimal referrerReward;

    @Column(name = "referee_reward", precision = 19, scale = 4)
    private BigDecimal refereeReward;

    @Column(name = "rewarded_at")
    private Instant rewardedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
