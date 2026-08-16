package com.fintech.ewallet.withdrawal.application;

import com.fintech.ewallet.withdrawal.application.dto.WithdrawalRequestResponse;
import com.fintech.ewallet.withdrawal.domain.WithdrawalRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetMyWithdrawalRequestsUseCase {

    private final WithdrawalRequestRepository withdrawalRequestRepository;

    @Transactional(readOnly = true)
    public List<WithdrawalRequestResponse> execute(UUID userId) {
        return withdrawalRequestRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(req -> {
                    // Lazy expire check
                    if (req.isExpired()) {
                        req.expire();
                        withdrawalRequestRepository.save(req);
                    }
                    return new WithdrawalRequestResponse(
                            req.getId(),
                            req.getWithdrawalCode(),
                            req.getAmount(),
                            req.getCurrency(),
                            req.getStatus(),
                            req.getExpiresAt(),
                            req.getCreatedAt()
                    );
                })
                .collect(Collectors.toList());
    }
}
