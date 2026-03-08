package com.fintech.ewallet.wallet.application.dto;

import com.fintech.ewallet.wallet.domain.Currency;
import com.fintech.ewallet.wallet.domain.TransferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO after a transfer is executed.
 */
public record ExecuteTransferResponse(
        UUID transferId,
        String referenceNo,
        String recipientDisplayName,
        BigDecimal amount,
        BigDecimal feeAmount,
        BigDecimal totalDeducted,
        Currency currency,
        TransferStatus status,
        Instant completedAt) {
}
