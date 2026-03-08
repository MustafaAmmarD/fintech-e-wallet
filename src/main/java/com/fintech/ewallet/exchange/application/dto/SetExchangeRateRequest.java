package com.fintech.ewallet.exchange.application.dto;

import com.fintech.ewallet.wallet.domain.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SetExchangeRateRequest(
        @NotNull(message = "From currency is required") Currency fromCurrency,
        @NotNull(message = "To currency is required") Currency toCurrency,
        @NotNull(message = "Rate is required") @DecimalMin(value = "0.00000001", message = "Rate must be positive") BigDecimal rate) {
}
