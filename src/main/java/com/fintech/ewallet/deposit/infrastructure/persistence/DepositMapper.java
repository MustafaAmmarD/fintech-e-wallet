package com.fintech.ewallet.deposit.infrastructure.persistence;

import com.fintech.ewallet.deposit.domain.Deposit;
import com.fintech.ewallet.deposit.domain.DepositStatus;
import com.fintech.ewallet.wallet.domain.Currency;
import org.springframework.stereotype.Component;

@Component
public class DepositMapper {

    public Deposit toDomain(DepositJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Deposit(
                entity.getId(),
                entity.getReferenceNo(),
                entity.getUserId(),
                entity.getAgentId(),
                entity.getWalletId(),
                entity.getAmount(),
                Currency.valueOf(entity.getCurrency()),
                entity.getDescription(),
                DepositStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt());
    }

    public DepositJpaEntity toEntity(Deposit deposit) {
        if (deposit == null) {
            return null;
        }
        return new DepositJpaEntity(
                deposit.getId(),
                deposit.getReferenceNo(),
                deposit.getUserId(),
                deposit.getAgentId(),
                deposit.getWalletId(),
                deposit.getAmount(),
                deposit.getCurrency().name(),
                deposit.getDescription(),
                deposit.getStatus().name(),
                deposit.getCreatedAt());
    }
}
