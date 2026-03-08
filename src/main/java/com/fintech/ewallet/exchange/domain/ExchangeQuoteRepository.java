package com.fintech.ewallet.exchange.domain;

import java.util.Optional;
import java.util.UUID;

public interface ExchangeQuoteRepository {

    ExchangeQuote save(ExchangeQuote exchangeQuote);

    Optional<ExchangeQuote> findById(UUID id);
}
