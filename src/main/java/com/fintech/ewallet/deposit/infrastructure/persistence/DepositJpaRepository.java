package com.fintech.ewallet.deposit.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DepositJpaRepository extends JpaRepository<DepositJpaEntity, UUID> {

    List<DepositJpaEntity> findByAgentIdOrderByCreatedAtDesc(UUID agentId, Pageable pageable);
}
