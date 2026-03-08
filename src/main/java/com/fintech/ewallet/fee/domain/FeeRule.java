package com.fintech.ewallet.fee.domain;

import com.fintech.ewallet.wallet.domain.Currency;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

public class FeeRule {

    private static final int MONEY_SCALE = 4;

    private final UUID id;
    private final FeeOperation operationType;
    private final Currency currency;
    private final FeeType feeType;
    private final BigDecimal rate;
    private final BigDecimal flatAmount;
    private final BigDecimal minAmount;
    private final BigDecimal maxAmount;
    private final BigDecimal minFee;
    private final BigDecimal maxFee;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    public FeeRule(
            UUID id,
            FeeOperation operationType,
            Currency currency,
            FeeType feeType,
            BigDecimal rate,
            BigDecimal flatAmount,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            BigDecimal minFee,
            BigDecimal maxFee,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.operationType = operationType;
        this.currency = currency;
        this.feeType = feeType;
        this.rate = rate;
        this.flatAmount = flatAmount;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.minFee = minFee;
        this.maxFee = maxFee;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static FeeRule create(
            FeeOperation operationType,
            Currency currency,
            FeeType feeType,
            BigDecimal rate,
            BigDecimal flatAmount,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            BigDecimal minFee,
            BigDecimal maxFee,
            boolean active) {
        Instant now = Instant.now();
        return new FeeRule(
                UUID.randomUUID(),
                operationType,
                currency,
                feeType,
                rate,
                flatAmount,
                minAmount,
                maxAmount,
                minFee,
                maxFee,
                active,
                now,
                now);
    }

    public boolean matchesAmount(BigDecimal amount) {
        if (amount.compareTo(minAmount) < 0) {
            return false;
        }
        return maxAmount == null || amount.compareTo(maxAmount) <= 0;
    }

    public BigDecimal calculate(BigDecimal amount) {
        BigDecimal fee = switch (feeType) {
            case PERCENTAGE -> amount.multiply(rate);
            case FLAT -> flatAmount;
        };

        fee = fee.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        if (minFee != null && fee.compareTo(minFee) < 0) {
            fee = minFee;
        }
        if (maxFee != null && fee.compareTo(maxFee) > 0) {
            fee = maxFee;
        }

        return fee.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public UUID getId() {
        return id;
    }

    public FeeOperation getOperationType() {
        return operationType;
    }

    public Currency getCurrency() {
        return currency;
    }

    public FeeType getFeeType() {
        return feeType;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public BigDecimal getFlatAmount() {
        return flatAmount;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public BigDecimal getMinFee() {
        return minFee;
    }

    public BigDecimal getMaxFee() {
        return maxFee;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
