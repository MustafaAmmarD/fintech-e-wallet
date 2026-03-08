package com.fintech.ewallet.limits.infrastructure.persistence;

import com.fintech.ewallet.limits.domain.TransactionLimit;
import org.springframework.stereotype.Component;

@Component
public class TransactionLimitMapper {

    public TransactionLimit toDomain(TransactionLimitJpaEntity entity) {
        TransactionLimit domain = new TransactionLimit();
        domain.setId(entity.getId());
        domain.setUserTier(entity.getUserTier());
        domain.setOperationType(entity.getOperationType());
        domain.setCurrency(entity.getCurrency());
        domain.setLimitType(entity.getLimitType());
        domain.setMaxAmount(entity.getMaxAmount());
        domain.setWindowHours(entity.getWindowHours());
        domain.setMaxCount(entity.getMaxCount());
        domain.setActive(entity.isActive());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        return domain;
    }
}
