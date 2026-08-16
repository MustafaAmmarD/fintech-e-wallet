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
import java.util.Optional;
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

        // 4. Find sender's wallet for the requested currency
        Wallet senderWallet = walletRepository.findByUserIdAndCurrency(senderUserId, request.currency())
                .orElseThrow(() -> new IllegalArgumentException("You don't have a " + request.currency() + " wallet"));

        // 5. Validate sender's wallet is active
        if (senderWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Your wallet is not active");
        }

        // 6. Cannot transfer from system wallets
        if (SystemWallets.isSystemWallet(senderWallet.getId())) {
            throw new IllegalArgumentException("System wallets cannot be used in P2P transfers");
        }

        String displayName = "Unregistered User (Pending Transfer)";
        if (recipient != null) {
            // Find recipient's wallet for the same currency
            Wallet recipientWallet = walletRepository.findByUserIdAndCurrency(recipient.getId(), request.currency())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Recipient doesn't have a " + request.currency() + " wallet"));
            
            if (recipientWallet.getStatus() != WalletStatus.ACTIVE) {
                throw new IllegalStateException("Recipient's wallet is not active");
            }
            if (SystemWallets.isSystemWallet(recipientWallet.getId())) {
                throw new IllegalArgumentException("System wallets cannot be used in P2P transfers");
            }
            displayName = nameMaskingService.getDisplayName(recipient, senderUserId);
        }

        // 7. Calculate fee
        BigDecimal feeAmount = calculateFeeUseCase.execute(FeeOperation.TRANSFER, request.currency(), request.amount());
        BigDecimal totalDeducted = request.amount().add(feeAmount);

        // 8. Check sufficient balance
        if (senderWallet.getBalance().compareTo(totalDeducted) < 0) {
            throw new IllegalStateException(
                    "Insufficient funds. Required: " + totalDeducted + " " + request.currency()
                            + " (Amount: " + request.amount() + " + Fee: " + feeAmount + ")"
                            + ", Available: " + senderWallet.getBalance());
        }

        BigDecimal senderBalanceAfter = senderWallet.getBalance().subtract(totalDeducted);

        // 9. Return preview
        return new TransferPreviewResponse(
                displayName,
                request.recipientAccountNumber() != null ? (recipient != null ? request.recipientAccountNumber() : fallbackPhone) : fallbackPhone,
                request.amount(),
                feeAmount,
                totalDeducted,
                request.currency(),
                senderBalanceAfter);
    }
}
