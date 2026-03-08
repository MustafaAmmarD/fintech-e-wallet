package com.fintech.ewallet.exchange.application.dto;

import com.fintech.ewallet.wallet.domain.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExchangeQuoteResponse(
        UUID quoteId,
        Currency fromCurrency,
        Currency toCurrency,
        BigDecimal fromAmount,
        BigDecimal toAmount,
        BigDecimal rate,
        BigDecimal feeAmount,
        BigDecimal totalDeducted,
        Instant expiresAt) {
}
