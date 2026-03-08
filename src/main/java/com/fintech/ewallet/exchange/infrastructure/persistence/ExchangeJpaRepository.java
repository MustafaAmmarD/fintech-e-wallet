package com.fintech.ewallet.exchange.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExchangeJpaRepository extends JpaRepository<ExchangeJpaEntity, UUID> {

    Optional<ExchangeJpaEntity> findByReferenceNo(String referenceNo);

    List<ExchangeJpaEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
