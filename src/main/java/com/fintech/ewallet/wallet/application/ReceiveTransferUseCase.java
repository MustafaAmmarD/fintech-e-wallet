package com.fintech.ewallet.wallet.application;

import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.shared.exception.TransferAlreadyClaimedException;
import com.fintech.ewallet.shared.exception.TransferNotFoundException;
import com.fintech.ewallet.wallet.application.dto.ReceiveTransferRequest;
import com.fintech.ewallet.wallet.domain.P2PTransfer;
import com.fintech.ewallet.wallet.domain.SystemWallets;
import com.fintech.ewallet.wallet.domain.TransferRepository;
import com.fintech.ewallet.wallet.domain.TransferStatus;
import com.fintech.ewallet.wallet.domain.Wallet;
import com.fintech.ewallet.wallet.domain.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiveTransferUseCase {

    private final TransferRepository transferRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final RecordLedgerEntryUseCase recordLedgerEntryUseCase;

    @Transactional
    public void execute(UUID userId, ReceiveTransferRequest request) {
        // 1. Fetch user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 2. Fetch the transfer
        P2PTransfer transfer = transferRepository.findById(UUID.fromString(request.processNumber()))
                .orElseThrow(() -> new TransferNotFoundException("Transfer not found"));

        // 3. Verify the transfer number matches
        if (!transfer.getReferenceNo().equals(request.transferNumber())) {
            throw new TransferNotFoundException("Invalid transfer reference number");
        }

        // 4. Check status
        if (transfer.getStatus() == TransferStatus.COMPLETED) {
            throw new TransferAlreadyClaimedException("This transfer has already been claimed.");
        }
        if (transfer.getStatus() != TransferStatus.PENDING && transfer.getStatus() != TransferStatus.UNCLAIMED) {
            throw new IllegalStateException("Transfer is not in a receivable state: " + transfer.getStatus());
        }

        // 5. Verify phone number
        if (transfer.getTargetPhoneNumber() != null && !transfer.getTargetPhoneNumber().equals(user.getPhoneNumber())) {
            throw new IllegalArgumentException("You are not authorized to receive this transfer.");
        }

        // 6. Get user's wallet for the currency
        Wallet recipientWallet = walletRepository.findByUserIdAndCurrency(user.getId(), transfer.getCurrency())
                .orElseThrow(() -> new IllegalArgumentException("You do not have a wallet for currency " + transfer.getCurrency()));

        // 7. Execute ledger movement (from liquidity/pending wallet to user wallet)
        // Note: The money was held in a liquidity system wallet while pending
        UUID liquidityWalletId = SystemWallets.getLiquidityWallet(transfer.getCurrency());
        
        UUID referenceId = UUID.randomUUID();
        recordLedgerEntryUseCase.recordDoubleEntry(
                liquidityWalletId,
                recipientWallet.getId(),
                transfer.getAmount(),
                com.fintech.ewallet.wallet.domain.ReferenceType.TRANSFER,
                referenceId,
                "Received pending transfer: " + transfer.getReferenceNo(),
                "استلام حوالة معلقة: " + transfer.getReferenceNo()
        );

        // 8. Update transfer
        transfer.receive(user.getId(), recipientWallet.getId());
        transferRepository.save(transfer);

        log.info("User {} received pending transfer {}", userId, transfer.getId());
    }
}
