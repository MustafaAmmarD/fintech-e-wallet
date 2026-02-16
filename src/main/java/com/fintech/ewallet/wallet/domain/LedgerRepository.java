package com.fintech.ewallet.wallet.domain;

import java.util.List;
import java.util.UUID;

public interface LedgerRepository {
    LedgerEntry save(LedgerEntry entry);

    List<LedgerEntry> saveAll(List<LedgerEntry> entries);

    List<LedgerEntry> findByWalletId(UUID walletId);

    List<LedgerEntry> findByTransactionId(UUID transactionId);

    List<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(UUID walletId, int limit);
}
