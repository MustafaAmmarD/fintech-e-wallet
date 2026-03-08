package com.fintech.ewallet.shared.exception;

public class ExchangeQuoteExpiredException extends DomainException {

    public ExchangeQuoteExpiredException() {
        super("EXCHANGE_QUOTE_EXPIRED", "Exchange quote expired. Please request a new quote.");
    }
}
