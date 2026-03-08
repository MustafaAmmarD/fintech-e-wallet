package com.fintech.ewallet.wallet.application.dto;

import com.fintech.ewallet.wallet.domain.Currency;
import com.fintech.ewallet.wallet.domain.TransferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Detailed view of a single transfer — used for both GET /transfers/{id}
 * and items within GET /transfers/history.
 */
public record TransferDetailResponse(
        UUID transferId,
        String referenceNo,
        UUID senderUserId,
        String senderDisplayName,
        UUID recipientUserId,
        String recipientDisplayName,
        BigDecimal amount,
        BigDecimal feeAmount,
        BigDecimal totalDeducted,
        Currency currency,
        TransferStatus status,
        String description,
        Instant createdAt,
        Instant completedAt) {
}
