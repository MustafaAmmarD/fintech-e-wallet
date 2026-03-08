package com.fintech.ewallet.withdrawal.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WithdrawalRepository {

    Withdrawal save(Withdrawal withdrawal);

    Optional<Withdrawal> findById(UUID id);

    List<Withdrawal> findByAgentIdOrderByCreatedAtDesc(UUID agentId, int limit);
}
