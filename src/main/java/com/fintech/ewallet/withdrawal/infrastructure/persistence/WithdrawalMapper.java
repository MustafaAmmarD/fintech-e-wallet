package com.fintech.ewallet.withdrawal.infrastructure.persistence;

import com.fintech.ewallet.wallet.domain.Currency;
import com.fintech.ewallet.withdrawal.domain.Withdrawal;
import com.fintech.ewallet.withdrawal.domain.WithdrawalStatus;
import org.springframework.stereotype.Component;

@Component
public class WithdrawalMapper {

    public Withdrawal toDomain(WithdrawalJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Withdrawal(
                entity.getId(),
                entity.getReferenceNo(),
                entity.getUserId(),
                entity.getAgentId(),
                entity.getWalletId(),
                entity.getAmount(),
                Currency.valueOf(entity.getCurrency()),
                entity.getDescription(),
                WithdrawalStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt());
    }

    public WithdrawalJpaEntity toEntity(Withdrawal withdrawal) {
        if (withdrawal == null) {
            return null;
        }
        return new WithdrawalJpaEntity(
                withdrawal.getId(),
                withdrawal.getReferenceNo(),
                withdrawal.getUserId(),
                withdrawal.getAgentId(),
                withdrawal.getWalletId(),
                withdrawal.getAmount(),
                withdrawal.getCurrency().name(),
                withdrawal.getDescription(),
                withdrawal.getStatus().name(),
                withdrawal.getCreatedAt());
    }
}
