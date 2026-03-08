package com.fintech.ewallet.fee.domain;

import com.fintech.ewallet.wallet.domain.Currency;

import java.util.List;
import java.util.UUID;

public interface FeeRuleRepository {

    FeeRule save(FeeRule feeRule);

    List<FeeRule> findActiveByOperationAndCurrency(FeeOperation operationType, Currency currency);

    List<FeeRule> findAllByOperationAndCurrency(FeeOperation operationType, Currency currency);

    void deactivateAll(FeeOperation operationType, Currency currency);

    boolean existsById(UUID id);
}
