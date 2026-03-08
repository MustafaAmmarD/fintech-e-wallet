package com.fintech.ewallet.shared.exception;

import com.fintech.ewallet.wallet.domain.Currency;

/**
 * Thrown when no exchange rate is available for a currency pair.
 */
public class ExchangeRateNotFoundException extends DomainException {

    public ExchangeRateNotFoundException(Currency fromCurrency, Currency toCurrency) {
        super(
                "EXCHANGE_RATE_NOT_FOUND",
                "Exchange rate not found for pair " + fromCurrency + " -> " + toCurrency);
    }
}
