package com.fintech.ewallet.deposit.infrastructure.persistence;

import com.fintech.ewallet.deposit.domain.Deposit;
import com.fintech.ewallet.deposit.domain.DepositRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DepositRepositoryAdapter implements DepositRepository {

    private final DepositJpaRepository jpaRepository;
    private final DepositMapper mapper;

    @Override
    public Deposit save(Deposit deposit) {
        DepositJpaEntity entity = mapper.toEntity(deposit);
        DepositJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Deposit> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Deposit> findByAgentIdOrderByCreatedAtDesc(UUID agentId, int limit) {
        int clampedLimit = Math.min(Math.max(limit, 1), 50);
        return jpaRepository.findByAgentIdOrderByCreatedAtDesc(agentId, PageRequest.of(0, clampedLimit)).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
