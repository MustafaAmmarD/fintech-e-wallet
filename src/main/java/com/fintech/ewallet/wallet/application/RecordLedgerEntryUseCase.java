package com.fintech.ewallet.wallet.application;

import com.fintech.ewallet.wallet.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Use case for recording double-entry ledger transactions.
 * Ensures zero-sum validation and pessimistic locking for wallet balance
 * updates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordLedgerEntryUseCase {

    private final LedgerRepository ledgerRepository;
    private final WalletRepository walletRepository;

    /**
     * Records a simple transfer between two wallets with pessimistic locking.
     * 
     * @param fromWalletId  Source wallet
     * @param toWalletId    Destination wallet
     * @param amount        Amount to transfer
     * @param referenceType Type of transaction (TRANSFER, DEPOSIT, etc.)
     * @param referenceId   ID of the source transaction
     * @param description   Human-readable description
     * @return Transaction ID grouping the ledger entries
     */
    @Transactional
    public UUID recordDoubleEntry(
            UUID fromWalletId,
            UUID toWalletId,
            BigDecimal amount,
            ReferenceType referenceType,
            UUID referenceId,
            String description) {
        log.info("Recording double-entry: {} YER from {} to {}", amount, fromWalletId, toWalletId);

        // 1. Load wallets with pessimistic lock (FOR UPDATE)
        Wallet fromWallet = walletRepository.findById(fromWalletId)
                .orElseThrow(() -> new IllegalArgumentException("Source wallet not found"));

        Wallet toWallet = walletRepository.findById(toWalletId)
                .orElseThrow(() -> new IllegalArgumentException("Destination wallet not found"));

        // 2. Validate currency match
        if (fromWallet.getCurrency() != toWallet.getCurrency()) {
            throw new IllegalArgumentException("Currency mismatch: cannot transfer between different currencies");
        }

        // 3. Debit from source wallet (validates balance)
        fromWallet.debit(amount);

        // 4. Credit to destination wallet
        toWallet.credit(amount);

        // 5. Save updated wallets (pessimistic lock ensures no race condition)
        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);

        // 6. Create ledger entries
        UUID transactionId = UUID.randomUUID();
        Currency currency = fromWallet.getCurrency();

        List<LedgerEntry> entries = new ArrayList<>();

        // Debit entry (money leaving source wallet)
        entries.add(new LedgerEntry(
                transactionId,
                fromWalletId,
                EntryType.DEBIT,
                amount,
                fromWallet.getBalance(), // Balance after debit
                currency,
                referenceType,
                referenceId,
                description + " (sent)"));

        // Credit entry (money entering destination wallet)
        entries.add(new LedgerEntry(
                transactionId,
                toWalletId,
                EntryType.CREDIT,
                amount,
                toWallet.getBalance(), // Balance after credit
                currency,
                referenceType,
                referenceId,
                description + " (received)"));

        // 7. Save all ledger entries atomically
        ledgerRepository.saveAll(entries);

        // 8. Zero-sum validation (sanity check)
        validateZeroSum(entries);

        log.info("Double-entry recorded successfully. Transaction ID: {}", transactionId);
        return transactionId;
    }

    /**
     * Records a triple-entry transaction (e.g., transfer with fee).
     * 
     * @param fromWalletId   Source wallet
     * @param toWalletId     Destination wallet
     * @param transferAmount Amount to transfer
     * @param feeWalletId    System fee wallet
     * @param feeAmount      Fee amount
     * @param referenceId    Reference transaction ID
     * @param description    Description
     * @return Transaction ID
     */
    @Transactional
    public UUID recordTransferWithFee(
            UUID fromWalletId,
            UUID toWalletId,
            BigDecimal transferAmount,
            UUID feeWalletId,
            BigDecimal feeAmount,
            UUID referenceId,
            String description) {
        log.info("Recording transfer with fee: {} + {} fee", transferAmount, feeAmount);

        // 1. Load wallets with lock
        Wallet fromWallet = walletRepository.findById(fromWalletId)
                .orElseThrow(() -> new IllegalArgumentException("Source wallet not found"));
        Wallet toWallet = walletRepository.findById(toWalletId)
                .orElseThrow(() -> new IllegalArgumentException("Destination wallet not found"));
        Wallet feeWallet = walletRepository.findById(feeWalletId)
                .orElseThrow(() -> new IllegalArgumentException("Fee wallet not found"));

        // 2. Calculate total debit (transfer + fee)
        BigDecimal totalDebit = transferAmount.add(feeAmount);

        // 3. Update balances
        fromWallet.debit(totalDebit);
        toWallet.credit(transferAmount);
        feeWallet.credit(feeAmount);

        // 4. Save wallets
        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);
        walletRepository.save(feeWallet);

        // 5. Create ledger entries
        UUID transactionId = UUID.randomUUID();
        Currency currency = fromWallet.getCurrency();

        List<LedgerEntry> entries = new ArrayList<>();

        // Debit from sender (transfer + fee)
        entries.add(new LedgerEntry(
                transactionId,
                fromWalletId,
                EntryType.DEBIT,
                totalDebit,
                fromWallet.getBalance(),
                currency,
                ReferenceType.TRANSFER,
                referenceId,
                description + " (sent with fee)"));

        // Credit to recipient
        entries.add(new LedgerEntry(
                transactionId,
                toWalletId,
                EntryType.CREDIT,
                transferAmount,
                toWallet.getBalance(),
                currency,
                ReferenceType.TRANSFER,
                referenceId,
                description + " (received)"));

        // Credit to fee wallet
        entries.add(new LedgerEntry(
                transactionId,
                feeWalletId,
                EntryType.CREDIT,
                feeAmount,
                feeWallet.getBalance(),
                currency,
                ReferenceType.FEE,
                referenceId,
                "Transaction fee"));

        // 6. Save entries
        ledgerRepository.saveAll(entries);

        // 7. Validate zero-sum
        validateZeroSum(entries);

        log.info("Transfer with fee recorded. Transaction ID: {}", transactionId);
        return transactionId;
    }

    /**
     * Validates that all ledger entries sum to zero (double-entry rule).
     */
    private void validateZeroSum(List<LedgerEntry> entries) {
        BigDecimal sum = BigDecimal.ZERO;
        for (LedgerEntry entry : entries) {
            if (entry.getEntryType() == EntryType.DEBIT) {
                sum = sum.subtract(entry.getAmount());
            } else {
                sum = sum.add(entry.getAmount());
            }
        }

        if (sum.compareTo(BigDecimal.ZERO) != 0) {
            log.error("CRITICAL: Zero-sum validation failed! Sum = {}", sum);
            throw new IllegalStateException("Ledger entries do not balance to zero! This is a critical bug.");
        }
    }
}
