package com.fintech.ewallet.wallet.application;

import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.shared.privacy.NameMaskingService;
import com.fintech.ewallet.wallet.application.dto.TransferDetailResponse;
import com.fintech.ewallet.wallet.domain.P2PTransfer;
import com.fintech.ewallet.wallet.domain.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Use case: Get the authenticated user's transfer history (sent + received).
 */
@Service
@RequiredArgsConstructor
public class GetTransferHistoryUseCase {

    private final TransferRepository transferRepository;
    private final UserRepository userRepository;
    private final NameMaskingService nameMaskingService;

    public List<TransferDetailResponse> execute(UUID userId, int limit) {
        int clampedLimit = Math.min(Math.max(limit, 1), 50);

        List<P2PTransfer> transfers = transferRepository.findByUserIdOrderByCreatedAtDesc(userId, clampedLimit);

        return transfers.stream()
                .map(transfer -> toDetailResponse(transfer, userId))
                .toList();
    }

    private TransferDetailResponse toDetailResponse(P2PTransfer transfer, UUID requestingUserId) {
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
