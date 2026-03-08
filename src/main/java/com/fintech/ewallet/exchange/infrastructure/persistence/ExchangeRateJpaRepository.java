package com.fintech.ewallet.exchange.infrastructure.persistence;

import com.fintech.ewallet.wallet.domain.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExchangeRateJpaRepository extends JpaRepository<ExchangeRateJpaEntity, UUID> {

    Optional<ExchangeRateJpaEntity> findByFromCurrencyAndToCurrency(Currency fromCurrency, Currency toCurrency);

    List<ExchangeRateJpaEntity> findAllByOrderByFromCurrencyAscToCurrencyAsc();
}
