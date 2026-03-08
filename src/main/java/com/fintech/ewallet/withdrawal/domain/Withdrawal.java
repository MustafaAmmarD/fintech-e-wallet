package com.fintech.ewallet.withdrawal.domain;

import com.fintech.ewallet.wallet.domain.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Agent cash withdrawal record.
 */
public class Withdrawal {

    private final UUID id;
    private final String referenceNo;
    private final UUID userId;
    private final UUID agentId;
    private final UUID walletId;
    private final BigDecimal amount;
    private final Currency currency;
    private final String description;
    private final WithdrawalStatus status;
    private final Instant createdAt;

    // Constructor for new withdrawal
    public Withdrawal(
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
        this.status = WithdrawalStatus.COMPLETED;
        this.createdAt = Instant.now();
    }

    // Constructor for reconstruction from persistence
    public Withdrawal(
            UUID id,
            String referenceNo,
            UUID userId,
            UUID agentId,
            UUID walletId,
            BigDecimal amount,
            Currency currency,
            String description,
            WithdrawalStatus status,
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

    public WithdrawalStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static String generateReferenceNo() {
        String datePart = LocalDate.now().toString().replace("-", "");
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "WDR-" + datePart + "-" + randomPart;
    }
}
