package com.fintech.ewallet.shared.exception;

import com.fintech.ewallet.exchange.domain.QuoteStatus;

public class ExchangeQuoteNotPendingException extends DomainException {

    public ExchangeQuoteNotPendingException(QuoteStatus status) {
        super("EXCHANGE_QUOTE_NOT_PENDING", "Exchange quote is not pending. Current status: " + status);
    }
}
