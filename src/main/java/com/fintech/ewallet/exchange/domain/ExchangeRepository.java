package com.fintech.ewallet.exchange.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExchangeRepository {

    Exchange save(Exchange exchange);

    Optional<Exchange> findById(UUID id);

    Optional<Exchange> findByReferenceNo(String referenceNo);

    List<Exchange> findByUserIdOrderByCreatedAtDesc(UUID userId, int limit);
}
