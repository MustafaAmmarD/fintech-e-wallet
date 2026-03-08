package com.fintech.ewallet.wallet.application;

import com.fintech.ewallet.fee.application.CalculateFeeUseCase;
import com.fintech.ewallet.fee.domain.FeeOperation;
import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.shared.privacy.NameMaskingService;
import com.fintech.ewallet.wallet.application.dto.ExecuteTransferRequest;
import com.fintech.ewallet.wallet.application.dto.ExecuteTransferResponse;
import com.fintech.ewallet.wallet.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Use case: Execute a confirmed P2P transfer.
 * Re-validates everything, creates ledger entries (with fee), records the
 * transfer, and returns the result.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecuteTransferUseCase {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final RecordLedgerEntryUseCase recordLedgerEntryUseCase;
    private final TransferRepository transferRepository;
    private final CalculateFeeUseCase calculateFeeUseCase;
    private final NameMaskingService nameMaskingService;

    @Transactional
    public ExecuteTransferResponse execute(UUID senderUserId, ExecuteTransferRequest request) {
        // 1. Look up sender
        User sender = userRepository.findById(senderUserId)
                .orElseThrow(() -> new IllegalArgumentException("Sender user not found"));

        // 2. Look up recipient by account number
        User recipient = userRepository.findByAccountNumber(request.recipientAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No user found with account number: " + request.recipientAccountNumber()));

        // 3. Prevent self-transfer
        if (sender.getId().equals(recipient.getId())) {
            throw new IllegalArgumentException("Cannot transfer to yourself");
        }

        // 4. Find sender's wallet for the currency
        Wallet senderWallet = walletRepository.findByUserIdAndCurrency(senderUserId, request.currency())
                .orElseThrow(() -> new IllegalArgumentException("You don't have a " + request.currency() + " wallet"));

        // 5. Find recipient's wallet for the same currency
        Wallet recipientWallet = walletRepository.findByUserIdAndCurrency(recipient.getId(), request.currency())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Recipient doesn't have a " + request.currency() + " wallet"));

        // 6. Validate wallets are active
        if (senderWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Your wallet is not active");
        }
        if (recipientWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Recipient's wallet is not active");
        }

        // 7. Cannot transfer from/to system wallets
        if (SystemWallets.isSystemWallet(senderWallet.getId())
                || SystemWallets.isSystemWallet(recipientWallet.getId())) {
            throw new IllegalArgumentException("System wallets cannot be used in P2P transfers");
        }

        // 8. Calculate fee
        BigDecimal feeAmount = calculateFeeUseCase.execute(FeeOperation.TRANSFER, request.currency(), request.amount());

        // 9. Get fee wallet for the currency
        UUID feeWalletId = SystemWallets.getFeeWallet(request.currency());

        // 10. Normalize description
        String description = normalizeDescription(request.description());

        // 11. Execute the transfer via ledger (triple-entry: debit sender, credit
        // recipient, credit fee wallet)
        UUID referenceId = UUID.randomUUID();
        UUID transactionId = recordLedgerEntryUseCase.recordTransferWithFee(
                senderWallet.getId(),
                recipientWallet.getId(),
                request.amount(),
                feeWalletId,
                feeAmount, com.fintech.ewallet.wallet.domain.ReferenceType.TRANSFER, referenceId,
                description);

        log.info("Transfer executed: sender={}, recipient={}, amount={} {}, fee={}, transactionId={}",
                senderUserId, recipient.getId(), request.amount(), request.currency(), feeAmount, transactionId);

        // 12. Record the transfer in the transfers table
        P2PTransfer transfer = new P2PTransfer(
                senderUserId,
                senderWallet.getId(),
                recipient.getId(),
                recipientWallet.getId(),
                request.amount(),
                feeAmount,
                request.currency(),
                description,
                transactionId);

        P2PTransfer savedTransfer = transferRepository.save(transfer);

        // 13. Return response
        return new ExecuteTransferResponse(
                savedTransfer.getId(),
                savedTransfer.getReferenceNo(),
                nameMaskingService.getDisplayName(recipient, senderUserId),
                savedTransfer.getAmount(),
                savedTransfer.getFeeAmount(),
                savedTransfer.getTotalDeducted(),
                savedTransfer.getCurrency(),
                savedTransfer.getStatus(),
                savedTransfer.getCompletedAt());
    }

    private String normalizeDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return "P2P Transfer";
        }
        return description.trim();
    }
}

