package com.fintech.ewallet.wallet.application;

import com.fintech.ewallet.wallet.application.dto.TransferRequest;
import com.fintech.ewallet.wallet.application.dto.TransferResponse;
import com.fintech.ewallet.wallet.domain.ReferenceType;
import com.fintech.ewallet.wallet.domain.SystemWallets;
import com.fintech.ewallet.wallet.domain.Wallet;
import com.fintech.ewallet.wallet.domain.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Use case: transfer money between two user wallets.
 */
@Service
@RequiredArgsConstructor
public class TransferMoneyUseCase {

    private final WalletRepository walletRepository;
    private final RecordLedgerEntryUseCase recordLedgerEntryUseCase;

    @Transactional
    public TransferResponse execute(UUID authenticatedUserId, TransferRequest request) {
        Wallet fromWallet = walletRepository.findById(request.fromWalletId())
                .orElseThrow(() -> new IllegalArgumentException("Source wallet not found"));
        Wallet toWallet = walletRepository.findById(request.toWalletId())
                .orElseThrow(() -> new IllegalArgumentException("Destination wallet not found"));

        validateSourceWalletOwnership(fromWallet, authenticatedUserId);
        validateTransferTarget(toWallet);

        UUID referenceId = UUID.randomUUID();
        String description = normalizeDescription(request.description());

        UUID transactionId = recordLedgerEntryUseCase.recordDoubleEntry(
                request.fromWalletId(),
                request.toWalletId(),
                request.amount(),
                ReferenceType.TRANSFER,
                referenceId,
                description);

        return new TransferResponse(
                transactionId,
                referenceId,
                request.fromWalletId(),
                request.toWalletId(),
                fromWallet.getCurrency(),
                request.amount(),
                description,
                Instant.now());
    }

    private void validateSourceWalletOwnership(Wallet fromWallet, UUID authenticatedUserId) {
        UUID sourceWalletUserId = fromWallet.getUserId();
        if (sourceWalletUserId == null || !sourceWalletUserId.equals(authenticatedUserId)) {
            throw new IllegalArgumentException("Source wallet must belong to the authenticated user");
        }
        if (SystemWallets.isSystemWallet(fromWallet.getId())) {
            throw new IllegalArgumentException("System wallets cannot be used as source wallets");
        }
    }

    private void validateTransferTarget(Wallet toWallet) {
        if (toWallet.getUserId() == null || SystemWallets.isSystemWallet(toWallet.getId())) {
            throw new IllegalArgumentException("Destination wallet must be a user wallet");
        }
    }

    private String normalizeDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return "Wallet transfer";
        }
        return description.trim();
    }
}
