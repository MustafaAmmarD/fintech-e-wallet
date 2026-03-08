package com.fintech.ewallet.limits.infrastructure.persistence;

import com.fintech.ewallet.limits.domain.LimitOperationType;
import com.fintech.ewallet.limits.domain.UserTier;
import com.fintech.ewallet.wallet.domain.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionLimitJpaRepository extends JpaRepository<TransactionLimitJpaEntity, UUID> {

    List<TransactionLimitJpaEntity> findByUserTierAndOperationTypeAndCurrencyAndActiveTrue(
            UserTier userTier,
            LimitOperationType operationType,
            Currency currency);
}
