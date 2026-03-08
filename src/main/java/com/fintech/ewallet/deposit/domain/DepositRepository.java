package com.fintech.ewallet.deposit.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepositRepository {

    Deposit save(Deposit deposit);

    Optional<Deposit> findById(UUID id);

    List<Deposit> findByAgentIdOrderByCreatedAtDesc(UUID agentId, int limit);
}
