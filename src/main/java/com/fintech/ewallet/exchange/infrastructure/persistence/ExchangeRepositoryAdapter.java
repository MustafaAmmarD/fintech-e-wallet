package com.fintech.ewallet.exchange.infrastructure.persistence;

import com.fintech.ewallet.exchange.domain.Exchange;
import com.fintech.ewallet.exchange.domain.ExchangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ExchangeRepositoryAdapter implements ExchangeRepository {

    private final ExchangeJpaRepository jpaRepository;
    private final ExchangeMapper mapper;

    @Override
    public Exchange save(Exchange exchange) {
        ExchangeJpaEntity entity = mapper.toEntity(exchange);
        ExchangeJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Exchange> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Exchange> findByReferenceNo(String referenceNo) {
        return jpaRepository.findByReferenceNo(referenceNo).map(mapper::toDomain);
    }

    @Override
    public List<Exchange> findByUserIdOrderByCreatedAtDesc(UUID userId, int limit) {
        int clampedLimit = Math.min(Math.max(limit, 1), 50);
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, clampedLimit))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
