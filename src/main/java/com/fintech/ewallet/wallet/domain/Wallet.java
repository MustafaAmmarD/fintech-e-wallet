package com.fintech.ewallet.wallet.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Wallet {
    private final UUID id;
    private final UUID userId;
    private final Currency currency;
    private BigDecimal balance; // Cached balance
    private WalletStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    // Constructor for new wallet
    public Wallet(UUID userId, Currency currency) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.currency = currency;
        this.balance = BigDecimal.ZERO;
        this.status = WalletStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Reconstruct from DB
    public Wallet(UUID id, UUID userId, Currency currency, BigDecimal balance, WalletStatus status, Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.currency = currency;
        this.balance = balance;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Business Logic Methods

    public void credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        if (this.status != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Wallet is not active");
        }
        this.balance = this.balance.add(amount);
        this.updatedAt = Instant.now();
    }

    public void debit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        if (this.status != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Wallet is not active");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }
        this.balance = this.balance.subtract(amount);
        this.updatedAt = Instant.now();
    }

    public void freeze() {
        this.status = WalletStatus.FROZEN;
        this.updatedAt = Instant.now();
    }

    public void unfreeze() {
        this.status = WalletStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public WalletStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
