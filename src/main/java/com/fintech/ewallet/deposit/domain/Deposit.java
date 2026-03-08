package com.fintech.ewallet.deposit.domain;

import com.fintech.ewallet.wallet.domain.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Agent cash deposit record.
 */
public class Deposit {

    private final UUID id;
    private final String referenceNo;
    private final UUID userId;
    private final UUID agentId;
    private final UUID walletId;
    private final BigDecimal amount;
    private final Currency currency;
    private final String description;
    private final DepositStatus status;
    private final Instant createdAt;

    // Constructor for a new deposit
    public Deposit(
            UUID userId,
            UUID agentId,
            UUID walletId,
            BigDecimal amount,
            Currency currency,
            String description) {
        this.id = UUID.randomUUID();
        this.referenceNo = generateReferenceNo();
        this.userId = userId;
        this.agentId = agentId;
        this.walletId = walletId;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.status = DepositStatus.COMPLETED;
        this.createdAt = Instant.now();
    }

    // Constructor for reconstruction from persistence
    public Deposit(
            UUID id,
            String referenceNo,
            UUID userId,
            UUID agentId,
            UUID walletId,
            BigDecimal amount,
            Currency currency,
            String description,
            DepositStatus status,
            Instant createdAt) {
        this.id = id;
        this.referenceNo = referenceNo;
        this.userId = userId;
        this.agentId = agentId;
        this.walletId = walletId;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getAgentId() {
        return agentId;
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

    public String getDescription() {
        return description;
    }

    public DepositStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static String generateReferenceNo() {
        String datePart = LocalDate.now().toString().replace("-", "");
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "DEP-" + datePart + "-" + randomPart;
    }
}
