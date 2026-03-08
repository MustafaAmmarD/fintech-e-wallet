package com.fintech.ewallet.withdrawal.infrastructure.persistence;

import com.fintech.ewallet.withdrawal.domain.Withdrawal;
import com.fintech.ewallet.withdrawal.domain.WithdrawalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WithdrawalRepositoryAdapter implements WithdrawalRepository {

    private final WithdrawalJpaRepository jpaRepository;
    private final WithdrawalMapper mapper;

    @Override
    public Withdrawal save(Withdrawal withdrawal) {
        WithdrawalJpaEntity entity = mapper.toEntity(withdrawal);
        WithdrawalJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Withdrawal> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Withdrawal> findByAgentIdOrderByCreatedAtDesc(UUID agentId, int limit) {
        int clampedLimit = Math.min(Math.max(limit, 1), 50);
        return jpaRepository.findByAgentIdOrderByCreatedAtDesc(agentId, PageRequest.of(0, clampedLimit)).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
