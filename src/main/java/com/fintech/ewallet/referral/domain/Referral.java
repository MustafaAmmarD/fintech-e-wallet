package com.fintech.ewallet.referral.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Referral {

    private UUID id;
    private UUID referrerId;
    private UUID refereeId;
    private String referralCode;
    private ReferralStatus status;
    private BigDecimal referrerReward;
    private BigDecimal refereeReward;
    private Instant rewardedAt;
    private Instant createdAt;

    public static Referral create(UUID referrerId, UUID refereeId, String referralCode) {
        Referral referral = new Referral();
        referral.id = UUID.randomUUID();
        referral.referrerId = referrerId;
        referral.refereeId = refereeId;
        referral.referralCode = referralCode;
        referral.status = ReferralStatus.PENDING;
        referral.createdAt = Instant.now();
        return referral;
    }

    public void markRewarded(BigDecimal referrerReward, BigDecimal refereeReward) {
        this.status = ReferralStatus.REWARDED;
        this.referrerReward = referrerReward;
        this.refereeReward = refereeReward;
        this.rewardedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getReferrerId() {
        return referrerId;
    }

    public void setReferrerId(UUID referrerId) {
        this.referrerId = referrerId;
    }

    public UUID getRefereeId() {
        return refereeId;
    }

    public void setRefereeId(UUID refereeId) {
        this.refereeId = refereeId;
    }

    public String getReferralCode() {
        return referralCode;
    }

    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
    }

    public ReferralStatus getStatus() {
        return status;
    }

    public void setStatus(ReferralStatus status) {
        this.status = status;
    }

    public BigDecimal getReferrerReward() {
        return referrerReward;
    }

    public void setReferrerReward(BigDecimal referrerReward) {
        this.referrerReward = referrerReward;
    }

    public BigDecimal getRefereeReward() {
        return refereeReward;
    }

    public void setRefereeReward(BigDecimal refereeReward) {
        this.refereeReward = refereeReward;
    }

    public Instant getRewardedAt() {
        return rewardedAt;
    }

    public void setRewardedAt(Instant rewardedAt) {
        this.rewardedAt = rewardedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
