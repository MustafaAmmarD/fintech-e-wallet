package com.fintech.ewallet.wallet.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * LedgerEntry represents a single entry in the double-entry ledger.
 * IMMUTABLE: Once created, ledger entries can NEVER be updated or deleted.
 */
public class LedgerEntry {
    private final UUID id;
    private final UUID transactionId; // Groups related entries (e.g., both sides of a transfer)
    private final UUID walletId;
    private final EntryType entryType;
    private final BigDecimal amount; // Always positive, sign determined by entryType
    private final BigDecimal balanceAfter; // Wallet balance AFTER this entry (audit trail)
    private final Currency currency;
    private final ReferenceType referenceType;
    private final UUID referenceId; // ID of the transfer/deposit/etc.
    private final String description;
    private final Instant createdAt;

    // Constructor for new entry
    public LedgerEntry(
            UUID transactionId,
            UUID walletId,
            EntryType entryType,
            BigDecimal amount,
            BigDecimal balanceAfter,
            Currency currency,
            ReferenceType referenceType,
            UUID referenceId,
            String description) {
        this.id = UUID.randomUUID();
        this.transactionId = transactionId;
        this.walletId = walletId;
        this.entryType = entryType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.currency = currency;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.description = description;
        this.createdAt = Instant.now();
    }

    // Reconstruct from DB
    public LedgerEntry(
            UUID id,
            UUID transactionId,
            UUID walletId,
            EntryType entryType,
            BigDecimal amount,
            BigDecimal balanceAfter,
            Currency currency,
            ReferenceType referenceType,
            UUID referenceId,
            String description,
            Instant createdAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.walletId = walletId;
        this.entryType = entryType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.currency = currency;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.description = description;
        this.createdAt = createdAt;
    }

    // Getters only (IMMUTABLE)
    public UUID getId() {
        return id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public EntryType getEntryType() {
        return entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public Currency getCurrency() {
        return currency;
    }

    public ReferenceType getReferenceType() {
        return referenceType;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
