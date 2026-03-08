package com.fintech.ewallet.fee.infrastructure.persistence;

import com.fintech.ewallet.fee.domain.FeeOperation;
import com.fintech.ewallet.fee.domain.FeeRule;
import com.fintech.ewallet.fee.domain.FeeRuleRepository;
import com.fintech.ewallet.wallet.domain.Currency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FeeRuleRepositoryAdapter implements FeeRuleRepository {

    private final FeeRuleJpaRepository jpaRepository;
    private final FeeRuleMapper mapper;

    @Override
    public FeeRule save(FeeRule feeRule) {
        FeeRuleJpaEntity entity = mapper.toEntity(feeRule);
        FeeRuleJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<FeeRule> findActiveByOperationAndCurrency(FeeOperation operationType, Currency currency) {
        return jpaRepository.findByOperationTypeAndCurrencyAndActiveTrueOrderByMinAmountDesc(operationType, currency)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<FeeRule> findAllByOperationAndCurrency(FeeOperation operationType, Currency currency) {
        return jpaRepository.findByOperationTypeAndCurrencyOrderByMinAmountDesc(operationType, currency)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deactivateAll(FeeOperation operationType, Currency currency) {
        jpaRepository.deactivateAllByOperationAndCurrency(operationType, currency, Instant.now());
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }
}
