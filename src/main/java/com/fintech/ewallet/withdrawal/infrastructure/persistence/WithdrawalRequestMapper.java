package com.fintech.ewallet.withdrawal.infrastructure.persistence;

import com.fintech.ewallet.withdrawal.domain.WithdrawalRequest;
import org.springframework.stereotype.Component;

@Component
public class WithdrawalRequestMapper {

    public WithdrawalRequest toDomain(WithdrawalRequestJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return new WithdrawalRequest(
                entity.getId(),
                entity.getUserId(),
                entity.getWalletId(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getWithdrawalCode(),
                entity.getStatus(),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                entity.getRedeemedAt()
        );
    }

    public WithdrawalRequestJpaEntity toEntity(WithdrawalRequest domain) {
        if (domain == null) {
            return null;
        }

        WithdrawalRequestJpaEntity entity = new WithdrawalRequestJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setWalletId(domain.getWalletId());
        entity.setAmount(domain.getAmount());
        entity.setCurrency(domain.getCurrency());
        entity.setWithdrawalCode(domain.getWithdrawalCode());
        entity.setStatus(domain.getStatus());
        entity.setExpiresAt(domain.getExpiresAt());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setRedeemedAt(domain.getRedeemedAt());

        return entity;
    }
}
