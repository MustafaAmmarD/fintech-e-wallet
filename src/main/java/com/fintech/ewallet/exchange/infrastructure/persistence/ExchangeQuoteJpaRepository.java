package com.fintech.ewallet.exchange.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ExchangeQuoteJpaRepository extends JpaRepository<ExchangeQuoteJpaEntity, UUID> {
}
