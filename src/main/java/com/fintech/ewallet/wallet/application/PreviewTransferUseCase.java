package com.fintech.ewallet.wallet.application;

import com.fintech.ewallet.fee.application.CalculateFeeUseCase;
import com.fintech.ewallet.fee.domain.FeeOperation;
import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.shared.privacy.NameMaskingService;
import com.fintech.ewallet.wallet.application.dto.TransferPreviewRequest;
import com.fintech.ewallet.wallet.application.dto.TransferPreviewResponse;
import com.fintech.ewallet.wallet.domain.SystemWallets;
import com.fintech.ewallet.wallet.domain.Wallet;
import com.fintech.ewallet.wallet.domain.WalletRepository;
import com.fintech.ewallet.wallet.domain.WalletStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Use case: Preview a P2P transfer before execution.
 * Validates everything and returns fee breakdown + recipient info, but does NOT
 * move money.
 */
@Service
@RequiredArgsConstructor
public class PreviewTransferUseCase {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final CalculateFeeUseCase calculateFeeUseCase;
    private final NameMaskingService nameMaskingService;

    public TransferPreviewResponse execute(UUID senderUserId, TransferPreviewRequest request) {
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

        // 4. Find sender's wallet for the requested currency
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
        BigDecimal totalDeducted = request.amount().add(feeAmount);

        // 9. Check sufficient balance
        if (senderWallet.getBalance().compareTo(totalDeducted) < 0) {
            throw new IllegalStateException(
                    "Insufficient funds. Required: " + totalDeducted + " " + request.currency()
                            + " (Amount: " + request.amount() + " + Fee: " + feeAmount + ")"
                            + ", Available: " + senderWallet.getBalance());
        }

        BigDecimal senderBalanceAfter = senderWallet.getBalance().subtract(totalDeducted);

        // 10. Return preview
        return new TransferPreviewResponse(
                nameMaskingService.getDisplayName(recipient, senderUserId),
                request.recipientAccountNumber(),
                request.amount(),
                feeAmount,
                totalDeducted,
                request.currency(),
                senderBalanceAfter);
    }
}
