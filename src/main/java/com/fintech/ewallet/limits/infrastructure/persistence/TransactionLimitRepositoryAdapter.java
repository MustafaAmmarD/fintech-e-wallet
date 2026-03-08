package com.fintech.ewallet.limits.infrastructure.persistence;

import com.fintech.ewallet.limits.domain.LimitOperationType;
import com.fintech.ewallet.limits.domain.TransactionLimit;
import com.fintech.ewallet.limits.domain.TransactionLimitRepository;
import com.fintech.ewallet.limits.domain.UserTier;
import com.fintech.ewallet.wallet.domain.Currency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TransactionLimitRepositoryAdapter implements TransactionLimitRepository {

    private final TransactionLimitJpaRepository jpaRepository;
    private final TransactionLimitMapper mapper;

    @Override
    public List<TransactionLimit> findActiveByCriteria(UserTier userTier, LimitOperationType operationType,
            Currency currency) {
        return jpaRepository.findByUserTierAndOperationTypeAndCurrencyAndActiveTrue(userTier, operationType, currency)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
