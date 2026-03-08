package com.fintech.ewallet.exchange.domain;

import com.fintech.ewallet.wallet.domain.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Time-locked quote used before exchange execution.
 */
public class ExchangeQuote {

    private final UUID id;
    private final UUID userId;
    private final Currency fromCurrency;
    private final Currency toCurrency;
    private final BigDecimal fromAmount;
    private final BigDecimal toAmount;
    private final BigDecimal rate;
    private final BigDecimal feeAmount;
    private final BigDecimal totalDeducted;
    private QuoteStatus status;
    private final Instant expiresAt;
    private final Instant createdAt;

    public ExchangeQuote(
            UUID userId,
            Currency fromCurrency,
            Currency toCurrency,
            BigDecimal fromAmount,
            BigDecimal toAmount,
            BigDecimal rate,
            BigDecimal feeAmount,
            BigDecimal totalDeducted,
            Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.fromAmount = fromAmount;
        this.toAmount = toAmount;
        this.rate = rate;
        this.feeAmount = feeAmount;
        this.totalDeducted = totalDeducted;
        this.status = QuoteStatus.PENDING;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public ExchangeQuote(
            UUID id,
            UUID userId,
            Currency fromCurrency,
            Currency toCurrency,
            BigDecimal fromAmount,
            BigDecimal toAmount,
            BigDecimal rate,
            BigDecimal feeAmount,
            BigDecimal totalDeducted,
            QuoteStatus status,
            Instant expiresAt,
            Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.fromAmount = fromAmount;
        this.toAmount = toAmount;
        this.rate = rate;
        this.feeAmount = feeAmount;
        this.totalDeducted = totalDeducted;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public void markExecuted() {
        this.status = QuoteStatus.EXECUTED;
    }

    public void markExpired() {
        this.status = QuoteStatus.EXPIRED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public Currency getFromCurrency() {
        return fromCurrency;
    }

    public Currency getToCurrency() {
        return toCurrency;
    }

    public BigDecimal getFromAmount() {
        return fromAmount;
    }

    public BigDecimal getToAmount() {
        return toAmount;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public BigDecimal getTotalDeducted() {
        return totalDeducted;
    }

    public QuoteStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
