package com.fintech.ewallet.exchange.infrastructure.persistence;

import com.fintech.ewallet.exchange.domain.Exchange;
import com.fintech.ewallet.exchange.domain.ExchangeStatus;
import com.fintech.ewallet.wallet.domain.Currency;
import org.springframework.stereotype.Component;

@Component
public class ExchangeMapper {

    public Exchange toDomain(ExchangeJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Exchange(
                entity.getId(),
                entity.getReferenceNo(),
                entity.getQuoteId(),
                entity.getUserId(),
                Currency.valueOf(entity.getFromCurrency()),
                Currency.valueOf(entity.getToCurrency()),
                entity.getFromAmount(),
                entity.getToAmount(),
                entity.getRateAtQuote(),
                entity.getRateAtExecute(),
                entity.getSlippageBps(),
                entity.getFeeAmount(),
                entity.getTotalDeducted(),
                ExchangeStatus.valueOf(entity.getStatus()),
                entity.getTransactionId(),
                entity.getCreatedAt());
    }

    public ExchangeJpaEntity toEntity(Exchange exchange) {
        if (exchange == null) {
            return null;
        }
        return new ExchangeJpaEntity(
                exchange.getId(),
                exchange.getReferenceNo(),
                exchange.getQuoteId(),
                exchange.getUserId(),
                exchange.getFromCurrency().name(),
                exchange.getToCurrency().name(),
                exchange.getFromAmount(),
                exchange.getToAmount(),
                exchange.getRateAtQuote(),
                exchange.getRateAtExecute(),
                exchange.getSlippageBps(),
                exchange.getFeeAmount(),
                exchange.getTotalDeducted(),
                exchange.getStatus().name(),
                exchange.getTransactionId(),
                exchange.getCreatedAt());
    }
}
