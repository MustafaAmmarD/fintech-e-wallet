package com.fintech.ewallet.exchange.application.dto;

import com.fintech.ewallet.exchange.domain.ExchangeStatus;
import com.fintech.ewallet.wallet.domain.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExchangeHistoryResponse(
        UUID exchangeId,
        String referenceNo,
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
        Instant createdAt) {
}
