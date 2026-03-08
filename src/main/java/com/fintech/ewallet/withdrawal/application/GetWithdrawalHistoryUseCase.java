package com.fintech.ewallet.withdrawal.application;

import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.shared.privacy.NameMaskingService;
import com.fintech.ewallet.withdrawal.application.dto.AgentWithdrawResponse;
import com.fintech.ewallet.withdrawal.domain.Withdrawal;
import com.fintech.ewallet.withdrawal.domain.WithdrawalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetWithdrawalHistoryUseCase {

    private final WithdrawalRepository withdrawalRepository;
    private final UserRepository userRepository;
    private final NameMaskingService nameMaskingService;

    public List<AgentWithdrawResponse> execute(UUID agentId, int limit) {
        int clampedLimit = Math.min(Math.max(limit, 1), 50);

        List<Withdrawal> withdrawals = withdrawalRepository.findByAgentIdOrderByCreatedAtDesc(agentId, clampedLimit);

        return withdrawals.stream()
                .map(withdrawal -> toResponse(withdrawal, agentId))
                .toList();
    }

    private AgentWithdrawResponse toResponse(Withdrawal withdrawal, UUID requesterId) {
        String userName = userRepository.findById(withdrawal.getUserId())
                .map(user -> nameMaskingService.getDisplayName(user, requesterId))
                .orElse("Unknown");

        return new AgentWithdrawResponse(
                withdrawal.getId(),
                withdrawal.getReferenceNo(),
                userName,
                withdrawal.getAmount(),
                withdrawal.getCurrency(),
                withdrawal.getStatus(),
                withdrawal.getCreatedAt());
    }
}
