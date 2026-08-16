package com.fintech.ewallet.withdrawal.application;

import com.fintech.ewallet.withdrawal.domain.WithdrawalRequest;
import com.fintech.ewallet.withdrawal.domain.WithdrawalRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CancelWithdrawalRequestUseCase {

    private final WithdrawalRequestRepository withdrawalRequestRepository;

    @Transactional
    public void execute(UUID userId, UUID requestId) {
        WithdrawalRequest request = withdrawalRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Withdrawal request not found"));

        if (!request.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized to cancel this request");
        }

        request.cancel();
        withdrawalRequestRepository.save(request);
    }
}
