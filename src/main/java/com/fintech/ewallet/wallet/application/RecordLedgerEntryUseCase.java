package com.fintech.ewallet.wallet.application;

import com.fintech.ewallet.identity.domain.KycStatus;
import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.limits.application.ValidateTransactionLimitUseCase;
import com.fintech.ewallet.limits.domain.LimitOperationType;
import com.fintech.ewallet.shared.event.FinancialTransactionCompletedEvent;
import com.fintech.ewallet.wallet.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    private static final BigDecimal MIN_TRANSFER_AMOUNT = new BigDecimal("1");

    private final LedgerRepository ledgerRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final ValidateTransactionLimitUseCase validateTransactionLimitUseCase;
    private final ApplicationEventPublisher applicationEventPublisher;

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
        log.info("Recording double-entry transaction from {} to {} amount {}", fromWalletId, toWalletId, amount);

        validateWalletPair(fromWalletId, toWalletId);
        validateTransferAmount(amount);

        Map<UUID, Wallet> lockedWallets = lockWalletsInStableOrder(List.of(fromWalletId, toWalletId));
        Wallet fromWallet = requireWallet(lockedWallets, fromWalletId, "Source wallet not found");
        Wallet toWallet = requireWallet(lockedWallets, toWalletId, "Destination wallet not found");

        enforceKycVerifiedForOperation(referenceType, fromWallet, toWallet);
        validateSameCurrency(fromWallet, toWallet);
        applyConfiguredDebitLimits(fromWallet, amount, resolveOperationType(referenceType));

        fromWallet.debit(amount);
        toWallet.credit(amount);

        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);

        UUID transactionId = UUID.randomUUID();
        Currency currency = fromWallet.getCurrency();

        List<LedgerEntry> entries = new ArrayList<>();

        entries.add(new LedgerEntry(
                transactionId,
                fromWalletId,
                EntryType.DEBIT,
                amount,
                fromWallet.getBalance(),
                currency,
                referenceType,
                referenceId,
                description + " (sent)"));

        entries.add(new LedgerEntry(
                transactionId,
                toWalletId,
                EntryType.CREDIT,
                amount,
                toWallet.getBalance(),
                currency,
                referenceType,
                referenceId,
                description + " (received)"));

        ledgerRepository.saveAll(entries);
        validateZeroSum(entries);
        publishFinancialTransactionCompleted(referenceType, referenceId, fromWallet, toWallet, amount, description);

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
            ReferenceType referenceType,
            UUID referenceId,
            String description) {
        log.info("Recording transfer with fee from {} to {} amount {} fee {}", fromWalletId, toWalletId, transferAmount,
                feeAmount);

        validateWalletPair(fromWalletId, toWalletId);
        if (feeWalletId == null) {
            throw new IllegalArgumentException("Fee wallet ID is required");
        }
        validateTransferAmount(transferAmount);
        validatePositiveAmount(feeAmount, "Fee amount");

        BigDecimal totalDebit = transferAmount.add(feeAmount);

        Map<UUID, Wallet> lockedWallets = lockWalletsInStableOrder(List.of(fromWalletId, toWalletId, feeWalletId));
        Wallet fromWallet = requireWallet(lockedWallets, fromWalletId, "Source wallet not found");
        Wallet toWallet = requireWallet(lockedWallets, toWalletId, "Destination wallet not found");
        Wallet feeWallet = requireWallet(lockedWallets, feeWalletId, "Fee wallet not found");

        enforceKycVerifiedForOperation(referenceType, fromWallet, toWallet);
        validateSameCurrency(fromWallet, toWallet);
        validateSameCurrency(fromWallet, feeWallet);
        applyConfiguredDebitLimits(fromWallet, totalDebit, resolveOperationType(referenceType));

        fromWallet.debit(totalDebit);
        toWallet.credit(transferAmount);
        feeWallet.credit(feeAmount);

        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);
        walletRepository.save(feeWallet);

        UUID transactionId = UUID.randomUUID();
        Currency currency = fromWallet.getCurrency();

        List<LedgerEntry> entries = new ArrayList<>();

        entries.add(new LedgerEntry(
                transactionId,
                fromWalletId,
                EntryType.DEBIT,
                totalDebit,
                fromWallet.getBalance(),
                currency,
                referenceType,
                referenceId,
                description + " (sent with fee)"));

        entries.add(new LedgerEntry(
                transactionId,
                toWalletId,
                EntryType.CREDIT,
                transferAmount,
                toWallet.getBalance(),
                currency,
                referenceType,
                referenceId,
                description + " (received)"));

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

        ledgerRepository.saveAll(entries);
        validateZeroSum(entries);
        publishFinancialTransactionCompleted(referenceType, referenceId, fromWallet, toWallet, transferAmount,
                description);

        log.info("Transfer with fee recorded. Transaction ID: {}", transactionId);
        return transactionId;
    }

    /**
     * Records a cross-currency exchange with fee using 5 ledger entries.
     *
     * Source side:
     * - Debit user source wallet by (fromAmount + feeAmount)
     * - Credit liquidity source wallet by fromAmount
     * - Credit fee wallet by feeAmount
     *
     * Destination side:
     * - Debit liquidity destination wallet by toAmount
     * - Credit user destination wallet by toAmount
     */
    @Transactional
    public UUID recordExchange(
            UUID userSourceWalletId,
            UUID userDestinationWalletId,
            UUID liquiditySourceWalletId,
            UUID liquidityDestinationWalletId,
            UUID feeWalletId,
            BigDecimal fromAmount,
            BigDecimal toAmount,
            BigDecimal feeAmount,
            UUID referenceId,
            String description) {
        log.info("Recording exchange transaction from {} to {}. fromAmount={}, toAmount={}, feeAmount={}",
                userSourceWalletId, userDestinationWalletId, fromAmount, toAmount, feeAmount);

        List<UUID> walletIds = List.of(
                userSourceWalletId,
                userDestinationWalletId,
                liquiditySourceWalletId,
                liquidityDestinationWalletId,
                feeWalletId);

        if (walletIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("All wallet IDs are required");
        }
        if (walletIds.stream().distinct().count() != walletIds.size()) {
            throw new IllegalArgumentException("Exchange wallets must be different");
        }

        validatePositiveAmount(fromAmount, "From amount");
        validatePositiveAmount(toAmount, "To amount");
        validatePositiveAmount(feeAmount, "Fee amount");

        BigDecimal totalSourceDebit = fromAmount.add(feeAmount);

        Map<UUID, Wallet> lockedWallets = lockWalletsInStableOrder(walletIds);
        Wallet userSourceWallet = requireWallet(lockedWallets, userSourceWalletId, "User source wallet not found");
        Wallet userDestinationWallet = requireWallet(lockedWallets, userDestinationWalletId,
                "User destination wallet not found");
        Wallet liquiditySourceWallet = requireWallet(lockedWallets, liquiditySourceWalletId,
                "Liquidity source wallet not found");
        Wallet liquidityDestinationWallet = requireWallet(lockedWallets, liquidityDestinationWalletId,
                "Liquidity destination wallet not found");
        Wallet feeWallet = requireWallet(lockedWallets, feeWalletId, "Fee wallet not found");

        enforceKycVerifiedForOperation(ReferenceType.EXCHANGE, userSourceWallet, userDestinationWallet);
        validateSameCurrency(userSourceWallet, liquiditySourceWallet);
        validateSameCurrency(userSourceWallet, feeWallet);
        validateSameCurrency(userDestinationWallet, liquidityDestinationWallet);

        if (userSourceWallet.getCurrency() == userDestinationWallet.getCurrency()) {
            throw new IllegalArgumentException("Exchange requires different source and destination currencies");
        }

        applyConfiguredDebitLimits(userSourceWallet, totalSourceDebit, LimitOperationType.EXCHANGE);

        userSourceWallet.debit(totalSourceDebit);
        liquiditySourceWallet.credit(fromAmount);
        feeWallet.credit(feeAmount);
        liquidityDestinationWallet.debit(toAmount);
        userDestinationWallet.credit(toAmount);

        walletRepository.save(userSourceWallet);
        walletRepository.save(userDestinationWallet);
        walletRepository.save(liquiditySourceWallet);
        walletRepository.save(liquidityDestinationWallet);
        walletRepository.save(feeWallet);

        UUID transactionId = UUID.randomUUID();
        Currency sourceCurrency = userSourceWallet.getCurrency();
        Currency destinationCurrency = userDestinationWallet.getCurrency();

        List<LedgerEntry> entries = new ArrayList<>();

        entries.add(new LedgerEntry(
                transactionId,
                userSourceWalletId,
                EntryType.DEBIT,
                totalSourceDebit,
                userSourceWallet.getBalance(),
                sourceCurrency,
                ReferenceType.EXCHANGE,
                referenceId,
                description + " (source debit + fee)"));

        entries.add(new LedgerEntry(
                transactionId,
                liquiditySourceWalletId,
                EntryType.CREDIT,
                fromAmount,
                liquiditySourceWallet.getBalance(),
                sourceCurrency,
                ReferenceType.EXCHANGE,
                referenceId,
                description + " (source liquidity credit)"));

        entries.add(new LedgerEntry(
                transactionId,
                feeWalletId,
                EntryType.CREDIT,
                feeAmount,
                feeWallet.getBalance(),
                sourceCurrency,
                ReferenceType.FEE,
                referenceId,
                "Exchange fee"));

        entries.add(new LedgerEntry(
                transactionId,
                liquidityDestinationWalletId,
                EntryType.DEBIT,
                toAmount,
                liquidityDestinationWallet.getBalance(),
                destinationCurrency,
                ReferenceType.EXCHANGE,
                referenceId,
                description + " (destination liquidity debit)"));

        entries.add(new LedgerEntry(
                transactionId,
                userDestinationWalletId,
                EntryType.CREDIT,
                toAmount,
                userDestinationWallet.getBalance(),
                destinationCurrency,
                ReferenceType.EXCHANGE,
                referenceId,
                description + " (destination credit)"));

        ledgerRepository.saveAll(entries);
        validateZeroSum(entries);
        publishFinancialTransactionCompleted(ReferenceType.EXCHANGE, referenceId, userSourceWallet,
                userDestinationWallet, fromAmount, description);

        log.info("Exchange recorded successfully. Transaction ID: {}", transactionId);
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

    private void validateWalletPair(UUID fromWalletId, UUID toWalletId) {
        if (fromWalletId == null || toWalletId == null) {
            throw new IllegalArgumentException("Wallet IDs are required");
        }
        if (fromWalletId.equals(toWalletId)) {
            throw new IllegalArgumentException("Source and destination wallets must be different");
        }
    }

    private void validateTransferAmount(BigDecimal amount) {
        validatePositiveAmount(amount, "Amount");
        if (amount.compareTo(MIN_TRANSFER_AMOUNT) < 0) {
            throw new IllegalArgumentException("Amount must be at least " + MIN_TRANSFER_AMOUNT);
        }
    }

    private void validatePositiveAmount(BigDecimal amount, String fieldName) {
        if (amount == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }

    private void validateSameCurrency(Wallet leftWallet, Wallet rightWallet) {
        if (leftWallet.getCurrency() != rightWallet.getCurrency()) {
            throw new IllegalArgumentException("Currency mismatch: cannot transfer between different currencies");
        }
    }

    private void applyConfiguredDebitLimits(Wallet fromWallet, BigDecimal requestedDebit,
            LimitOperationType operationType) {
        if (operationType == null) {
            return;
        }

        validateTransactionLimitUseCase.validateDebit(
                fromWallet.getId(),
                fromWallet.getUserId(),
                fromWallet.getCurrency(),
                operationType,
                requestedDebit);
    }

    private void enforceKycVerifiedForOperation(ReferenceType referenceType, Wallet... wallets) {
        if (!requiresVerifiedKyc(referenceType) || wallets == null || wallets.length == 0) {
            return;
        }

        Set<UUID> seenUserIds = new HashSet<>();
        for (Wallet wallet : wallets) {
            if (wallet == null || wallet.getUserId() == null) {
                continue;
            }
            UUID userId = wallet.getUserId();
            if (!seenUserIds.add(userId)) {
                continue;
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalStateException("Wallet owner not found: " + userId));
            if (user.getKycStatus() != KycStatus.VERIFIED) {
                throw new IllegalStateException(
                        "KYC verification required before " + referenceType.name().toLowerCase() + " operations");
            }
        }
    }

    private boolean requiresVerifiedKyc(ReferenceType referenceType) {
        if (referenceType == null) {
            return false;
        }
        return switch (referenceType) {
            case TRANSFER, DEPOSIT, WITHDRAWAL, EXCHANGE -> true;
            default -> false;
        };
    }

    private LimitOperationType resolveOperationType(ReferenceType referenceType) {
        if (referenceType == null) {
            return null;
        }
        return switch (referenceType) {
            case TRANSFER -> LimitOperationType.TRANSFER;
            case WITHDRAWAL -> LimitOperationType.WITHDRAWAL;
            case EXCHANGE -> LimitOperationType.EXCHANGE;
            default -> null;
        };
    }

    private void publishFinancialTransactionCompleted(
            ReferenceType referenceType,
            UUID referenceId,
            Wallet primarySourceWallet,
            Wallet primaryDestinationWallet,
            BigDecimal primaryAmount,
            String description) {

        if (!isReferralTriggerReferenceType(referenceType)) {
            return;
        }

        Set<UUID> userIds = new HashSet<>();
        UUID initiatorId = null;
        String initiatorName = null;

        if (primarySourceWallet != null && primarySourceWallet.getUserId() != null) {
            userIds.add(primarySourceWallet.getUserId());
            initiatorId = primarySourceWallet.getUserId();
            User user = userRepository.findById(initiatorId).orElse(null);
            if (user != null) {
                initiatorName = user.getFullName(); // Use domain directly for internal events
            }
        }
        if (primaryDestinationWallet != null && primaryDestinationWallet.getUserId() != null) {
            userIds.add(primaryDestinationWallet.getUserId());
        }

        if (userIds.isEmpty()) {
            return;
        }

        // For agent deposits, initiator is the system/agent, recipient gets the money.
        if (referenceType == ReferenceType.DEPOSIT) {
            initiatorId = null; // System/Agent initiated
            initiatorName = "System/Agent";
        }

        applicationEventPublisher.publishEvent(new FinancialTransactionCompletedEvent(
                referenceType.name(),
                referenceId,
                userIds,
                initiatorId,
                initiatorName,
                primaryAmount,
                primarySourceWallet != null ? primarySourceWallet.getCurrency().name() : "YER"));
    }

    private boolean isReferralTriggerReferenceType(ReferenceType referenceType) {
        if (referenceType == null) {
            return false;
        }
        return switch (referenceType) {
            case TRANSFER, DEPOSIT, WITHDRAWAL, EXCHANGE -> true;
            default -> false;
        };
    }

    private Map<UUID, Wallet> lockWalletsInStableOrder(List<UUID> walletIds) {
        List<UUID> sortedWalletIds = walletIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        Map<UUID, Wallet> lockedWallets = new LinkedHashMap<>();
        for (UUID walletId : sortedWalletIds) {
            Wallet wallet = walletRepository.findByIdForUpdate(walletId)
                    .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletId));
            lockedWallets.put(walletId, wallet);
        }
        return lockedWallets;
    }

    private Wallet requireWallet(Map<UUID, Wallet> lockedWallets, UUID walletId, String notFoundMessage) {
        Wallet wallet = lockedWallets.get(walletId);
        if (wallet == null) {
            throw new IllegalArgumentException(notFoundMessage);
        }
        return wallet;
    }
}
