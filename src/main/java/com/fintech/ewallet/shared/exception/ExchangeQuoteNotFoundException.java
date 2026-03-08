package com.fintech.ewallet.shared.exception;

import java.util.UUID;

public class ExchangeQuoteNotFoundException extends DomainException {

    public ExchangeQuoteNotFoundException(UUID quoteId) {
        super("EXCHANGE_QUOTE_NOT_FOUND", "Exchange quote not found: " + quoteId);
    }
}
