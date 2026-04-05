package com.fintech.ewallet.limits.domain;

import com.fintech.ewallet.wallet.domain.Currency;

import java.util.List;

public interface TransactionLimitRepository {

    List<TransactionLimit> findActiveByCriteria(UserTier userTier, LimitOperationType operationType, Currency currency);

    List<TransactionLimit> findAllActiveByUserTier(UserTier userTier);
}
