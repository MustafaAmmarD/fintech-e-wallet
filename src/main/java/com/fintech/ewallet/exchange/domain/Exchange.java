package com.fintech.ewallet.exchange.domain;

import com.fintech.ewallet.wallet.domain.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class Exchange {

    private final UUID id;
    private final String referenceNo;
    private final UUID quoteId;
    private final UUID userId;
    private final Currency fromCurrency;
    private final Currency toCurrency;
    private final BigDecimal fromAmount;
    private final BigDecimal toAmount;
    private final BigDecimal rateAtQuote;
    private final BigDecimal rateAtExecute;
    private final BigDecimal slippageBps;
    private final BigDecimal feeAmount;
    private final BigDecimal totalDeducted;
    private final ExchangeStatus status;
    private final UUID transactionId;
    private final Instant createdAt;

    public Exchange(
            UUID quoteId,
            UUID userId,
            Currency fromCurrency,
            Currency toCurrency,
            BigDecimal fromAmount,
            BigDecimal toAmount,
            BigDecimal rateAtQuote,
            BigDecimal rateAtExecute,
            BigDecimal slippageBps,
            BigDecimal feeAmount,
            BigDecimal totalDeducted,
            UUID transactionId) {
        this(
                quoteId,
                userId,
                fromCurrency,
                toCurrency,
                fromAmount,
                toAmount,
                rateAtQuote,
                rateAtExecute,
                slippageBps,
                feeAmount,
                totalDeducted,
                ExchangeStatus.COMPLETED,
                transactionId);
    }

    public Exchange(
            UUID quoteId,
            UUID userId,
            Currency fromCurrency,
            Currency toCurrency,
            BigDecimal fromAmount,
            BigDecimal toAmount,
            BigDecimal rateAtQuote,
            BigDecimal rateAtExecute,
            BigDecimal slippageBps,
            BigDecimal feeAmount,
            BigDecimal totalDeducted,
            ExchangeStatus status,
            UUID transactionId) {
        this.id = UUID.randomUUID();
        this.referenceNo = generateReferenceNo();
        this.quoteId = quoteId;
        this.userId = userId;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.fromAmount = fromAmount;
        this.toAmount = toAmount;
        this.rateAtQuote = rateAtQuote;
        this.rateAtExecute = rateAtExecute;
        this.slippageBps = slippageBps;
        this.feeAmount = feeAmount;
        this.totalDeducted = totalDeducted;
        this.status = status;
        this.transactionId = transactionId;
        this.createdAt = Instant.now();
    }

    public Exchange(
            UUID id,
            String referenceNo,
            UUID quoteId,
            UUID userId,
            Currency fromCurrency,
            Currency toCurrency,
            BigDecimal fromAmount,
            BigDecimal toAmount,
            BigDecimal rateAtQuote,
            BigDecimal rateAtExecute,
            BigDecimal slippageBps,
            BigDecimal feeAmount,
            BigDecimal totalDeducted,
            ExchangeStatus status,
            UUID transactionId,
            Instant createdAt) {
        this.id = id;
        this.referenceNo = referenceNo;
        this.quoteId = quoteId;
        this.userId = userId;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.fromAmount = fromAmount;
        this.toAmount = toAmount;
        this.rateAtQuote = rateAtQuote;
        this.rateAtExecute = rateAtExecute;
        this.slippageBps = slippageBps;
        this.feeAmount = feeAmount;
        this.totalDeducted = totalDeducted;
        this.status = status;
        this.transactionId = transactionId;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public UUID getQuoteId() {
        return quoteId;
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

    public BigDecimal getRateAtQuote() {
        return rateAtQuote;
    }

    public BigDecimal getRateAtExecute() {
        return rateAtExecute;
    }

    public BigDecimal getSlippageBps() {
        return slippageBps;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public BigDecimal getTotalDeducted() {
        return totalDeducted;
    }

    public ExchangeStatus getStatus() {
        return status;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static String generateReferenceNo() {
        String datePart = LocalDate.now().toString().replace("-", "");
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "EXC-" + datePart + "-" + randomPart;
    }
}
