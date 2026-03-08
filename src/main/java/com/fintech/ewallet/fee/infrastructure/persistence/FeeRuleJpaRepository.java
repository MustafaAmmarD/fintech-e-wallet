package com.fintech.ewallet.fee.infrastructure.persistence;

import com.fintech.ewallet.fee.domain.FeeOperation;
import com.fintech.ewallet.wallet.domain.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface FeeRuleJpaRepository extends JpaRepository<FeeRuleJpaEntity, UUID> {

    List<FeeRuleJpaEntity> findByOperationTypeAndCurrencyAndActiveTrueOrderByMinAmountDesc(
            FeeOperation operationType,
            Currency currency);

    List<FeeRuleJpaEntity> findByOperationTypeAndCurrencyOrderByMinAmountDesc(
            FeeOperation operationType,
            Currency currency);

    @Modifying
    @Query("UPDATE FeeRuleJpaEntity f SET f.active = false, f.updatedAt = :updatedAt WHERE f.operationType = :operationType AND f.currency = :currency")
    void deactivateAllByOperationAndCurrency(
            @Param("operationType") FeeOperation operationType,
            @Param("currency") Currency currency,
            @Param("updatedAt") Instant updatedAt);
}
