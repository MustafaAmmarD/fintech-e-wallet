package com.fintech.ewallet.exchange.application.dto;

import com.fintech.ewallet.wallet.domain.Currency;

import java.math.BigDecimal;
import java.time.Instant;

public record ExchangeRateResponse(
        Currency fromCurrency,
        Currency toCurrency,
        BigDecimal rate,
        Instant effectiveAt) {
}
