package com.fintech.ewallet.wallet.application;

import com.fintech.ewallet.wallet.application.dto.ReconciliationResult;
import com.fintech.ewallet.wallet.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Use case for reconciling cached balance with ledger-calculated balance.
 * This helps detect bugs where the cached balance becomes stale.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconcileBalanceUseCase {

    private final WalletRepository walletRepository;
    private final LedgerRepository ledgerRepository;

    /**
     * Reconcile a single wallet.
     */
    public ReconciliationResult execute(UUID walletId) {
        // 1. Get cached balance
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
        BigDecimal cachedBalance = wallet.getBalance();

        // 2. Calculate balance from ledger (source of truth)
        BigDecimal ledgerBalance = calculateBalanceFromLedger(walletId);

        // 3. Compare
        boolean matches = cachedBalance.compareTo(ledgerBalance) == 0;
        BigDecimal discrepancy = cachedBalance.subtract(ledgerBalance);

        // 4. Alert if mismatch
        if (!matches) {
            log.error("BALANCE MISMATCH DETECTED! Wallet: {}, Cached: {}, Ledger: {}, Discrepancy: {}",
                    walletId, cachedBalance, ledgerBalance, discrepancy);
            // In production: Send alert, email, Slack notification, freeze wallet, etc.
        } else {
            log.info("Balance reconciliation OK for wallet: {}", walletId);
        }

        return new ReconciliationResult(
                walletId,
                cachedBalance,
                ledgerBalance,
                matches,
                discrepancy);
    }

    /**
     * Reconcile all wallets for a user.
     */
    public List<ReconciliationResult> executeForUser(UUID userId) {
        List<Wallet> wallets = walletRepository.findByUserId(userId);
        return wallets.stream()
                .map(wallet -> execute(wallet.getId()))
                .toList();
    }

    /**
     * Calculate balance by summing all ledger entries for a wallet.
     * DEBIT = negative, CREDIT = positive.
     */
    private BigDecimal calculateBalanceFromLedger(UUID walletId) {
        List<LedgerEntry> entries = ledgerRepository.findByWalletId(walletId);

        BigDecimal sum = BigDecimal.ZERO;
        for (LedgerEntry entry : entries) {
            if (entry.getEntryType() == EntryType.DEBIT) {
                sum = sum.subtract(entry.getAmount());
            } else {
                sum = sum.add(entry.getAmount());
            }
        }

        return sum;
    }
}
