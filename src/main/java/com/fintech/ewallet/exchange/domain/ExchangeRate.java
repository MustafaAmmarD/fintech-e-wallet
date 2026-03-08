package com.fintech.ewallet.exchange.domain;

import com.fintech.ewallet.wallet.domain.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Exchange rate for one directional currency pair.
 */
public class ExchangeRate {

    private final UUID id;
    private final Currency fromCurrency;
    private final Currency toCurrency;
    private final BigDecimal rate;
    private final UUID setBy;
    private final Instant effectiveAt;
    private final Instant createdAt;

    private ExchangeRate(
            UUID id,
            Currency fromCurrency,
            Currency toCurrency,
            BigDecimal rate,
            UUID setBy,
            Instant effectiveAt,
            Instant createdAt) {
        this.id = id;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.rate = rate;
        this.setBy = setBy;
        this.effectiveAt = effectiveAt;
        this.createdAt = createdAt;
    }

    public static ExchangeRate createNew(
            Currency fromCurrency,
            Currency toCurrency,
            BigDecimal rate,
            UUID setBy,
            Instant effectiveAt) {
        Instant now = Instant.now();
        return new ExchangeRate(
                UUID.randomUUID(),
                fromCurrency,
                toCurrency,
                rate,
                setBy,
                effectiveAt,
                now);
    }

    public static ExchangeRate reconstruct(
            UUID id,
            Currency fromCurrency,
            Currency toCurrency,
            BigDecimal rate,
            UUID setBy,
            Instant effectiveAt,
            Instant createdAt) {
        return new ExchangeRate(id, fromCurrency, toCurrency, rate, setBy, effectiveAt, createdAt);
    }

    public ExchangeRate withUpdatedRate(BigDecimal newRate, UUID newSetBy, Instant newEffectiveAt) {
        return new ExchangeRate(id, fromCurrency, toCurrency, newRate, newSetBy, newEffectiveAt, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public Currency getFromCurrency() {
        return fromCurrency;
    }

    public Currency getToCurrency() {
        return toCurrency;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public UUID getSetBy() {
        return setBy;
    }

    public Instant getEffectiveAt() {
        return effectiveAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
