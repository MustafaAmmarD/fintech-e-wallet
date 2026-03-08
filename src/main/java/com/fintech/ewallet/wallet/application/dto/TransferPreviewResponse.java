package com.fintech.ewallet.wallet.application.dto;

import com.fintech.ewallet.wallet.domain.Currency;

import java.math.BigDecimal;

/**
 * Response DTO for transfer preview — shows what will happen before
 * confirmation.
 */
public record TransferPreviewResponse(
        String recipientDisplayName,
        String recipientAccountNumber,
        BigDecimal amount,
        BigDecimal feeAmount,
        BigDecimal totalDeducted,
        Currency currency,
        BigDecimal senderBalanceAfter) {
}
