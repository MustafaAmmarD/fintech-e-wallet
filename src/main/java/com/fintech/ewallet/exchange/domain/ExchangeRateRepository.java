package com.fintech.ewallet.exchange.domain;

import com.fintech.ewallet.wallet.domain.Currency;

import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository {

    ExchangeRate save(ExchangeRate exchangeRate);

    Optional<ExchangeRate> findByCurrencyPair(Currency fromCurrency, Currency toCurrency);

    List<ExchangeRate> findAll();
}
