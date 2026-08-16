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
import java.util.Optional;
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
        if ((request.recipientAccountNumber() == null || request.recipientAccountNumber().trim().isEmpty()) &&
            (request.targetPhoneNumber() == null || request.targetPhoneNumber().trim().isEmpty())) {
            throw new IllegalArgumentException("Either recipient account number or target phone number must be provided");
        }

        // 1. Look up sender
        User sender = userRepository.findById(senderUserId)
                .orElseThrow(() -> new IllegalArgumentException("Sender user not found"));

        // 2. Look up recipient
        User recipient = null;
        String fallbackPhone = null;

        if (request.recipientAccountNumber() != null && !request.recipientAccountNumber().trim().isEmpty()) {
            String input = request.recipientAccountNumber().trim();
            Optional<User> byAccount = userRepository.findByAccountNumber(input);
            if (byAccount.isPresent()) {
                recipient = byAccount.get();
            } else {
                // Fallback: Flutter app might send a phone number in the account number field
                String phoneStr = input.startsWith("+") ? input : "+967" + input;
                recipient = userRepository.findByPhoneNumber(phoneStr).orElse(null);
                if (recipient == null) fallbackPhone = phoneStr;
            }
        } else if (request.targetPhoneNumber() != null && !request.targetPhoneNumber().trim().isEmpty()) {
            String phoneStr = request.targetPhoneNumber().trim();
            phoneStr = phoneStr.startsWith("+") ? phoneStr : "+967" + phoneStr;
            recipient = userRepository.findByPhoneNumber(phoneStr).orElse(null);
            if (recipient == null) fallbackPhone = phoneStr;
        }

        // 3. Prevent self-transfer
        if (recipient != null && sender.getId().equals(recipient.getId())) {
            throw new IllegalArgumentException("Cannot transfer to yourself");
        }

        // 4. Find sender's wallet for the currency
        Wallet senderWallet = walletRepository.findByUserIdAndCurrency(senderUserId, request.currency())
                .orElseThrow(() -> new IllegalArgumentException("You don't have a " + request.currency() + " wallet"));

        // 5. Validate sender's wallet is active
        if (senderWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Your wallet is not active");
        }
        if (SystemWallets.isSystemWallet(senderWallet.getId())) {
            throw new IllegalArgumentException("System wallets cannot be used in P2P transfers");
        }

        Wallet recipientWallet = null;
        if (recipient != null) {
            // Find recipient's wallet for the same currency
            recipientWallet = walletRepository.findByUserIdAndCurrency(recipient.getId(), request.currency())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Recipient doesn't have a " + request.currency() + " wallet"));

            if (recipientWallet.getStatus() != WalletStatus.ACTIVE) {
                throw new IllegalStateException("Recipient's wallet is not active");
            }
            if (SystemWallets.isSystemWallet(recipientWallet.getId())) {
                throw new IllegalArgumentException("System wallets cannot be used in P2P transfers");
            }
        }

        // 6. Calculate fee
        BigDecimal feeAmount = calculateFeeUseCase.execute(FeeOperation.TRANSFER, request.currency(), request.amount());

        // 7. Get fee wallet for the currency
        UUID feeWalletId = SystemWallets.getFeeWallet(request.currency());

        // 8. Normalize description
        String description = normalizeDescription(request.description(), "P2P Transfer");
        String descriptionAr = normalizeDescription(request.description(), "حوالة من شخص لشخص");

        // 9. Execute the transfer via ledger
        UUID referenceId = UUID.randomUUID();
        UUID transactionId;
        
        UUID destinationWalletId;
        if (recipientWallet != null) {
            destinationWalletId = recipientWallet.getId();
        } else {
            // Pending transfer: send to liquidity wallet until claimed
            destinationWalletId = SystemWallets.getLiquidityWallet(request.currency());
            description = normalizeDescription(request.description(), "Pending Transfer");
            descriptionAr = normalizeDescription(request.description(), "حوالة معلقة");
        }

        transactionId = recordLedgerEntryUseCase.recordTransferWithFee(
                senderWallet.getId(),
                destinationWalletId,
                request.amount(),
                feeWalletId,
                feeAmount, 
                com.fintech.ewallet.wallet.domain.ReferenceType.TRANSFER, 
                referenceId,
                description,
                descriptionAr);

        log.info("Transfer executed: sender={}, recipient={}, amount={} {}, fee={}, transactionId={}",
                senderUserId, recipient != null ? recipient.getId() : "PENDING", request.amount(), request.currency(), feeAmount, transactionId);

        // 10. Record the transfer in the transfers table
        P2PTransfer transfer;
        if (recipient != null) {
            transfer = new P2PTransfer(
                    senderUserId,
                    senderWallet.getId(),
                    recipient.getId(),
                    recipientWallet.getId(),
                    request.amount(),
                    feeAmount,
                    request.currency(),
                    description,
                    transactionId);
        } else {
            transfer = new P2PTransfer(
                    senderUserId,
                    senderWallet.getId(),
                    sender.getPhoneNumber(),
                    null,
                    null,
                    fallbackPhone,
                    request.amount(),
                    feeAmount,
                    request.currency(),
                    description,
                    transactionId,
                    TransferStatus.PENDING);
        }

        P2PTransfer savedTransfer = transferRepository.save(transfer);

        String displayName = recipient != null ? nameMaskingService.getDisplayName(recipient, senderUserId) : "Unregistered User (Pending Transfer)";

        // 11. Return response
        return new ExecuteTransferResponse(
                savedTransfer.getId(),
                savedTransfer.getReferenceNo(),
                displayName,
                savedTransfer.getAmount(),
                savedTransfer.getFeeAmount(),
                savedTransfer.getTotalDeducted(),
                savedTransfer.getCurrency(),
                savedTransfer.getStatus(),
                savedTransfer.getCompletedAt() != null ? savedTransfer.getCompletedAt() : savedTransfer.getCreatedAt());
    }

    private String normalizeDescription(String description, String defaultValue) {
        if (description == null || description.trim().isEmpty()) {
            return defaultValue;
        }
        return description.trim();
    }
}
