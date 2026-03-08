package com.fintech.ewallet.exchange.infrastructure.persistence;

import com.fintech.ewallet.exchange.domain.ExchangeQuote;
import com.fintech.ewallet.exchange.domain.ExchangeQuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ExchangeQuoteRepositoryAdapter implements ExchangeQuoteRepository {

    private final ExchangeQuoteJpaRepository jpaRepository;
    private final ExchangeQuoteMapper mapper;

    @Override
    public ExchangeQuote save(ExchangeQuote exchangeQuote) {
        ExchangeQuoteJpaEntity entity = mapper.toEntity(exchangeQuote);
        ExchangeQuoteJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ExchangeQuote> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }
}
