package com.fintech.ewallet.wallet.application;

import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.shared.exception.TransferAlreadyClaimedException;
import com.fintech.ewallet.shared.exception.TransferNotFoundException;
import com.fintech.ewallet.wallet.application.dto.CancelTransferRequest;
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
public class CancelTransferUseCase {

    private final TransferRepository transferRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final RecordLedgerEntryUseCase recordLedgerEntryUseCase;

    @Transactional
    public void execute(UUID userId, CancelTransferRequest request) {
        // 1. Fetch user (ensure exists)
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 2. Fetch the transfer
        P2PTransfer transfer = transferRepository.findById(UUID.fromString(request.processNumber()))
                .orElseThrow(() -> new TransferNotFoundException("Transfer not found"));

        // 3. Verify the transfer number matches
        if (!transfer.getReferenceNo().equals(request.transferNumber())) {
            throw new TransferNotFoundException("Invalid transfer reference number");
        }

        // 4. Verify currency
        if (transfer.getCurrency() != request.currency()) {
            throw new IllegalArgumentException("Currency mismatch");
        }

        // 5. Verify the user is the sender
        if (transfer.getSenderUserId() == null || !transfer.getSenderUserId().equals(userId)) {
            throw new IllegalArgumentException("You are not authorized to cancel this transfer.");
        }

        // 6. Check status
        if (transfer.getStatus() == TransferStatus.COMPLETED) {
            throw new TransferAlreadyClaimedException("Cannot cancel: this transfer has already been claimed.");
        }
        if (transfer.getStatus() != TransferStatus.PENDING && transfer.getStatus() != TransferStatus.UNCLAIMED) {
            throw new IllegalStateException("Transfer is not in a cancellable state: " + transfer.getStatus());
        }

        // 7. Get sender's wallet for the currency
        Wallet senderWallet = walletRepository.findByUserIdAndCurrency(userId, transfer.getCurrency())
                .orElseThrow(() -> new IllegalArgumentException("You do not have a wallet for currency " + transfer.getCurrency()));

        // 8. Execute ledger movement (refund to sender from pending wallet)
        // Note: The total deducted amount (amount + fee) needs to be refunded.
        UUID liquidityWalletId = SystemWallets.getLiquidityWallet(transfer.getCurrency());
        
        UUID referenceId = UUID.randomUUID();
        // Since recordLedgerEntryUseCase.recordDeposit doesn't take fee into account for refund,
        // we can just deposit the totalDeducted back into the sender's wallet from the liquidity wallet.
        recordLedgerEntryUseCase.recordDoubleEntry(
                liquidityWalletId,
                senderWallet.getId(),
                transfer.getTotalDeducted(),
                com.fintech.ewallet.wallet.domain.ReferenceType.TRANSFER,
                referenceId,
                "Cancelled pending transfer: " + transfer.getReferenceNo(),
                "إلغاء حوالة معلقة: " + transfer.getReferenceNo()
        );

        // 9. Update transfer
        transfer.cancel(request.reason());
        transferRepository.save(transfer);

        log.info("User {} cancelled pending transfer {}", userId, transfer.getId());
    }
}
