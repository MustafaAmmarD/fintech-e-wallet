package com.fintech.ewallet.withdrawal.domain;

import com.fintech.ewallet.wallet.domain.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a user's request to withdraw cash via an agent.
 */
public class WithdrawalRequest {

    private final UUID id;
    private final UUID userId;
    private final UUID walletId;
    private final BigDecimal amount;
    private final Currency currency;
    private final String withdrawalCode;
    private WithdrawalRequestStatus status;
    private final Instant expiresAt;
    private final Instant createdAt;
    private Instant redeemedAt;

    // Constructor for creating a new request
    public WithdrawalRequest(
            UUID userId,
            UUID walletId,
            BigDecimal amount,
            Currency currency,
            String withdrawalCode,
            Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.walletId = walletId;
        this.amount = amount;
        this.currency = currency;
        this.withdrawalCode = withdrawalCode;
        this.status = WithdrawalRequestStatus.PENDING;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    // Constructor for reconstruction from persistence
    public WithdrawalRequest(
            UUID id,
            UUID userId,
            UUID walletId,
            BigDecimal amount,
            Currency currency,
            String withdrawalCode,
            WithdrawalRequestStatus status,
            Instant expiresAt,
            Instant createdAt,
            Instant redeemedAt) {
        this.id = id;
        this.userId = userId;
        this.walletId = walletId;
        this.amount = amount;
        this.currency = currency;
        this.withdrawalCode = withdrawalCode;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.redeemedAt = redeemedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public String getWithdrawalCode() {
        return withdrawalCode;
    }

    public WithdrawalRequestStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRedeemedAt() {
        return redeemedAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public void redeem() {
        if (this.status != WithdrawalRequestStatus.PENDING) {
            throw new IllegalStateException("Withdrawal request is not in PENDING state");
        }
        if (isExpired()) {
            this.status = WithdrawalRequestStatus.EXPIRED;
            throw new IllegalStateException("Withdrawal request has expired");
        }
        this.status = WithdrawalRequestStatus.REDEEMED;
        this.redeemedAt = Instant.now();
    }

    public void cancel() {
        if (this.status != WithdrawalRequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING withdrawal requests can be cancelled");
        }
        this.status = WithdrawalRequestStatus.CANCELLED;
    }

    public void expire() {
        if (this.status == WithdrawalRequestStatus.PENDING) {
            this.status = WithdrawalRequestStatus.EXPIRED;
        }
    }
}
