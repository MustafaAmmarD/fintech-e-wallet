package com.fintech.ewallet.wallet.domain;

import java.util.List;
import java.util.UUID;

public interface LedgerRepository {
    LedgerEntry save(LedgerEntry entry);

    List<LedgerEntry> saveAll(List<LedgerEntry> entries);

    /**
     * Find all ledger entries for a wallet.
     */
    List<LedgerEntry> findByWalletId(UUID walletId);

    /**
     * Find ledger entries for a wallet, ordered by creation date descending,
     * limited.
     */
    List<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(UUID walletId, int limit);

    /**
     * Find all ledger entries for a transaction.
     */
    List<LedgerEntry> findByTransactionId(UUID transactionId);
}
