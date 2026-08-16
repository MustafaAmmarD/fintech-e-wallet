package com.fintech.ewallet.withdrawal.infrastructure.persistence;

import com.fintech.ewallet.withdrawal.domain.WithdrawalRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WithdrawalRequestJpaRepository extends JpaRepository<WithdrawalRequestJpaEntity, UUID> {

    Optional<WithdrawalRequestJpaEntity> findByWithdrawalCode(String withdrawalCode);

    long countByUserIdAndStatus(UUID userId, WithdrawalRequestStatus status);

    List<WithdrawalRequestJpaEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
