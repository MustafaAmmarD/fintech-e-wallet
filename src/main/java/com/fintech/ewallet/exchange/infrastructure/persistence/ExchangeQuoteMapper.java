package com.fintech.ewallet.exchange.infrastructure.persistence;

import com.fintech.ewallet.exchange.domain.ExchangeQuote;
import org.springframework.stereotype.Component;

@Component
public class ExchangeQuoteMapper {

    public ExchangeQuote toDomain(ExchangeQuoteJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ExchangeQuote(
                entity.getId(),
                entity.getUserId(),
                entity.getFromCurrency(),
                entity.getToCurrency(),
                entity.getFromAmount(),
                entity.getToAmount(),
                entity.getRate(),
                entity.getFeeAmount(),
                entity.getTotalDeducted(),
                entity.getStatus(),
                entity.getExpiresAt(),
                entity.getCreatedAt());
    }

    public ExchangeQuoteJpaEntity toEntity(ExchangeQuote exchangeQuote) {
        if (exchangeQuote == null) {
            return null;
        }
        return new ExchangeQuoteJpaEntity(
                exchangeQuote.getId(),
                exchangeQuote.getUserId(),
                exchangeQuote.getFromCurrency(),
                exchangeQuote.getToCurrency(),
                exchangeQuote.getFromAmount(),
                exchangeQuote.getToAmount(),
                exchangeQuote.getRate(),
                exchangeQuote.getFeeAmount(),
                exchangeQuote.getTotalDeducted(),
                exchangeQuote.getStatus(),
                exchangeQuote.getExpiresAt(),
                exchangeQuote.getCreatedAt());
    }
}
