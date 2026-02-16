package com.fintech.ewallet.wallet.infrastructure.persistence;

import com.fintech.ewallet.wallet.domain.Wallet;
import org.springframework.stereotype.Component;

@Component
public class WalletMapper {

    public Wallet toDomain(WalletJpaEntity entity) {
        if (entity == null)
            return null;
        return new Wallet(
                entity.getId(),
                entity.getUserId(),
                entity.getCurrency(),
                entity.getBalance(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public WalletJpaEntity toEntity(Wallet wallet) {
        if (wallet == null)
            return null;
        return new WalletJpaEntity(
                wallet.getId(),
                wallet.getUserId(),
                wallet.getCurrency(),
                wallet.getBalance(),
                wallet.getStatus(),
                wallet.getCreatedAt(),
                java.time.Instant.now() // Always update timestamp on save
        );
    }
}
