package com.fintech.ewallet.wallet.application;

import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.shared.privacy.NameMaskingService;
import com.fintech.ewallet.wallet.application.dto.TransferDetailResponse;
import com.fintech.ewallet.wallet.domain.P2PTransfer;
import com.fintech.ewallet.wallet.domain.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Use case: Get details of a specific transfer.
 * Only the sender or recipient can view the transfer.
 */
@Service
@RequiredArgsConstructor
public class GetTransferDetailUseCase {

    private final TransferRepository transferRepository;
    private final UserRepository userRepository;
    private final NameMaskingService nameMaskingService;

    public TransferDetailResponse execute(UUID transferId, UUID requestingUserId) {
        P2PTransfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found"));

        // Only sender or recipient can view
        if (!transfer.getSenderUserId().equals(requestingUserId)
                && !transfer.getRecipientUserId().equals(requestingUserId)) {
            throw new SecurityException("Access denied: you are not a party to this transfer");
        }

        // Look up display names
        String senderName = userRepository.findById(transfer.getSenderUserId())
                .map(user -> nameMaskingService.getDisplayName(user, requestingUserId))
                .orElse("Unknown");

        String recipientName = userRepository.findById(transfer.getRecipientUserId())
                .map(user -> nameMaskingService.getDisplayName(user, requestingUserId))
                .orElse("Unknown");

        return new TransferDetailResponse(
                transfer.getId(),
                transfer.getReferenceNo(),
                transfer.getSenderUserId(),
                senderName,
                transfer.getRecipientUserId(),
                recipientName,
                transfer.getAmount(),
                transfer.getFeeAmount(),
                transfer.getTotalDeducted(),
                transfer.getCurrency(),
                transfer.getStatus(),
                transfer.getDescription(),
                transfer.getCreatedAt(),
                transfer.getCompletedAt());
    }
}
