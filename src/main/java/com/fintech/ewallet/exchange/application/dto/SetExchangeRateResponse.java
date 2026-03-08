package com.fintech.ewallet.exchange.application.dto;

import com.fintech.ewallet.wallet.domain.Currency;

import java.math.BigDecimal;
import java.time.Instant;

public record SetExchangeRateResponse(
        Currency fromCurrency,
        Currency toCurrency,
        BigDecimal rate,
        BigDecimal reverseRate,
        Instant effectiveAt) {
}
