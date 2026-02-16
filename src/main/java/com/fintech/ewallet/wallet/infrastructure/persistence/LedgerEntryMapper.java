package com.fintech.ewallet.wallet.infrastructure.persistence;

import com.fintech.ewallet.wallet.domain.LedgerEntry;
import org.springframework.stereotype.Component;

@Component
public class LedgerEntryMapper {

    public LedgerEntry toDomain(LedgerEntryJpaEntity entity) {
        if (entity == null)
            return null;
        return new LedgerEntry(
                entity.getId(),
                entity.getTransactionId(),
                entity.getWalletId(),
                entity.getEntryType(),
                entity.getAmount(),
                entity.getBalanceAfter(),
                entity.getCurrency(),
                entity.getReferenceType(),
                entity.getReferenceId(),
                entity.getDescription(),
                entity.getCreatedAt());
    }

    public LedgerEntryJpaEntity toEntity(LedgerEntry ledgerEntry) {
        if (ledgerEntry == null)
            return null;
        return new LedgerEntryJpaEntity(
                ledgerEntry.getId(),
                ledgerEntry.getTransactionId(),
                ledgerEntry.getWalletId(),
                ledgerEntry.getEntryType(),
                ledgerEntry.getAmount(),
                ledgerEntry.getBalanceAfter(),
                ledgerEntry.getCurrency(),
                ledgerEntry.getReferenceType(),
                ledgerEntry.getReferenceId(),
                ledgerEntry.getDescription(),
                ledgerEntry.getCreatedAt());
    }
}
