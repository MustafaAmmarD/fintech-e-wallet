package com.fintech.ewallet.limits.domain;

import com.fintech.ewallet.wallet.domain.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TransactionLimit {

    private UUID id;
    private UserTier userTier;
    private LimitOperationType operationType;
    private Currency currency;
    private LimitType limitType;
    private BigDecimal maxAmount;
    private Integer windowHours;
    private Integer maxCount;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UserTier getUserTier() {
        return userTier;
    }

    public void setUserTier(UserTier userTier) {
        this.userTier = userTier;
    }

    public LimitOperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(LimitOperationType operationType) {
        this.operationType = operationType;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public LimitType getLimitType() {
        return limitType;
    }

    public void setLimitType(LimitType limitType) {
        this.limitType = limitType;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public Integer getWindowHours() {
        return windowHours;
    }

    public void setWindowHours(Integer windowHours) {
        this.windowHours = windowHours;
    }

    public Integer getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(Integer maxCount) {
        this.maxCount = maxCount;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
