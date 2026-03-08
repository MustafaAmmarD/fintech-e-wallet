package com.fintech.ewallet.fee.infrastructure.persistence;

import com.fintech.ewallet.fee.domain.FeeRule;
import org.springframework.stereotype.Component;

@Component
public class FeeRuleMapper {

    public FeeRule toDomain(FeeRuleJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new FeeRule(
                entity.getId(),
                entity.getOperationType(),
                entity.getCurrency(),
                entity.getFeeType(),
                entity.getRate(),
                entity.getFlatAmount(),
                entity.getMinAmount(),
                entity.getMaxAmount(),
                entity.getMinFee(),
                entity.getMaxFee(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public FeeRuleJpaEntity toEntity(FeeRule feeRule) {
        if (feeRule == null) {
            return null;
        }
        return new FeeRuleJpaEntity(
                feeRule.getId(),
                feeRule.getOperationType(),
                feeRule.getCurrency(),
                feeRule.getFeeType(),
                feeRule.getRate(),
                feeRule.getFlatAmount(),
                feeRule.getMinAmount(),
                feeRule.getMaxAmount(),
                feeRule.getMinFee(),
                feeRule.getMaxFee(),
                feeRule.isActive(),
                feeRule.getCreatedAt(),
                feeRule.getUpdatedAt());
    }
}
