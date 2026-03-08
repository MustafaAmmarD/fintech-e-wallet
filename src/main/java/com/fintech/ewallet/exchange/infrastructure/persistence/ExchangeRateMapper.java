package com.fintech.ewallet.exchange.infrastructure.persistence;

import com.fintech.ewallet.exchange.domain.ExchangeRate;
import org.springframework.stereotype.Component;

@Component
public class ExchangeRateMapper {

    public ExchangeRate toDomain(ExchangeRateJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return ExchangeRate.reconstruct(
                entity.getId(),
                entity.getFromCurrency(),
                entity.getToCurrency(),
                entity.getRate(),
                entity.getSetBy(),
                entity.getEffectiveAt(),
                entity.getCreatedAt());
    }

    public ExchangeRateJpaEntity toEntity(ExchangeRate exchangeRate) {
        if (exchangeRate == null) {
            return null;
        }
        return new ExchangeRateJpaEntity(
                exchangeRate.getId(),
                exchangeRate.getFromCurrency(),
                exchangeRate.getToCurrency(),
                exchangeRate.getRate(),
                exchangeRate.getSetBy(),
                exchangeRate.getEffectiveAt(),
                exchangeRate.getCreatedAt());
    }
}
