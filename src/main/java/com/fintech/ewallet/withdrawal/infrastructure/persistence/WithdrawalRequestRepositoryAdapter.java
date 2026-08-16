package com.fintech.ewallet.withdrawal.infrastructure.persistence;

import com.fintech.ewallet.withdrawal.domain.WithdrawalRequest;
import com.fintech.ewallet.withdrawal.domain.WithdrawalRequestRepository;
import com.fintech.ewallet.withdrawal.domain.WithdrawalRequestStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class WithdrawalRequestRepositoryAdapter implements WithdrawalRequestRepository {

    private final WithdrawalRequestJpaRepository jpaRepository;
    private final WithdrawalRequestMapper mapper;

    @Override
    public WithdrawalRequest save(WithdrawalRequest withdrawalRequest) {
        WithdrawalRequestJpaEntity entity = mapper.toEntity(withdrawalRequest);
        WithdrawalRequestJpaEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<WithdrawalRequest> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<WithdrawalRequest> findByWithdrawalCode(String withdrawalCode) {
        return jpaRepository.findByWithdrawalCode(withdrawalCode).map(mapper::toDomain);
    }

    @Override
    public long countByUserIdAndStatus(UUID userId, WithdrawalRequestStatus status) {
        return jpaRepository.countByUserIdAndStatus(userId, status);
    }

    @Override
    public List<WithdrawalRequest> findByUserIdOrderByCreatedAtDesc(UUID userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
