package com.fintech.ewallet.withdrawal.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WithdrawalRequestRepository {

    WithdrawalRequest save(WithdrawalRequest withdrawalRequest);

    Optional<WithdrawalRequest> findById(UUID id);

    Optional<WithdrawalRequest> findByWithdrawalCode(String withdrawalCode);

    long countByUserIdAndStatus(UUID userId, WithdrawalRequestStatus status);

    List<WithdrawalRequest> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
