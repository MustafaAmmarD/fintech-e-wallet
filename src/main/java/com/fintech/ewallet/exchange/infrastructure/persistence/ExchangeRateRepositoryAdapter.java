package com.fintech.ewallet.exchange.infrastructure.persistence;

import com.fintech.ewallet.exchange.domain.ExchangeRate;
import com.fintech.ewallet.exchange.domain.ExchangeRateRepository;
import com.fintech.ewallet.wallet.domain.Currency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ExchangeRateRepositoryAdapter implements ExchangeRateRepository {

    private final ExchangeRateJpaRepository jpaRepository;
    private final ExchangeRateMapper mapper;

    @Override
    public ExchangeRate save(ExchangeRate exchangeRate) {
        ExchangeRateJpaEntity entity = mapper.toEntity(exchangeRate);
        ExchangeRateJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ExchangeRate> findByCurrencyPair(Currency fromCurrency, Currency toCurrency) {
        return jpaRepository.findByFromCurrencyAndToCurrency(fromCurrency, toCurrency)
                .map(mapper::toDomain);
    }

    @Override
    public List<ExchangeRate> findAll() {
        return jpaRepository.findAllByOrderByFromCurrencyAscToCurrencyAsc()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
