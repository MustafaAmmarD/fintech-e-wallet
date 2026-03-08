package com.fintech.ewallet.withdrawal.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WithdrawalJpaRepository extends JpaRepository<WithdrawalJpaEntity, UUID> {

    List<WithdrawalJpaEntity> findByAgentIdOrderByCreatedAtDesc(UUID agentId, Pageable pageable);
}
