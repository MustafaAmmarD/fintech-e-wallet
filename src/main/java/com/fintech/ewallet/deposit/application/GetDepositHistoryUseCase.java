package com.fintech.ewallet.deposit.application;

import com.fintech.ewallet.deposit.application.dto.AgentDepositResponse;
import com.fintech.ewallet.deposit.domain.Deposit;
import com.fintech.ewallet.deposit.domain.DepositRepository;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.shared.privacy.NameMaskingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetDepositHistoryUseCase {

    private final DepositRepository depositRepository;
    private final UserRepository userRepository;
    private final NameMaskingService nameMaskingService;

    public List<AgentDepositResponse> execute(UUID agentId, int limit) {
        int clampedLimit = Math.min(Math.max(limit, 1), 50);

        List<Deposit> deposits = depositRepository.findByAgentIdOrderByCreatedAtDesc(agentId, clampedLimit);

        return deposits.stream()
                .map(deposit -> toResponse(deposit, agentId))
                .toList();
    }

    private AgentDepositResponse toResponse(Deposit deposit, UUID requesterId) {
        String recipientName = userRepository.findById(deposit.getUserId())
                .map(user -> nameMaskingService.getDisplayName(user, requesterId))
                .orElse("Unknown");

        return new AgentDepositResponse(
                deposit.getId(),
                deposit.getReferenceNo(),
                recipientName,
                deposit.getAmount(),
                deposit.getCurrency(),
                deposit.getStatus(),
                deposit.getCreatedAt());
    }
}
