package com.fintech.ewallet.limits.application;

import com.fintech.ewallet.limits.application.dto.LimitResponse;
import com.fintech.ewallet.limits.domain.TransactionLimit;
import com.fintech.ewallet.limits.domain.TransactionLimitRepository;
import com.fintech.ewallet.limits.domain.UserTier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case: Get all active transaction limits for a given user tier.
 */
@Service
@RequiredArgsConstructor
public class GetTransactionLimitsUseCase {

    private final TransactionLimitRepository transactionLimitRepository;

    public List<LimitResponse> execute(UserTier userTier) {
        // Fetch all active limits across all operations and currencies for this tier
        return transactionLimitRepository.findAllActiveByUserTier(userTier).stream()
                .map(limit -> new LimitResponse(
                        limit.getOperationType(),
                        limit.getCurrency(),
                        limit.getUserTier(),
                        limit.getLimitType(),
                        limit.getMaxAmount(),
                        limit.getWindowHours(),
                        limit.getMaxCount()
                ))
                .toList();
    }
}
