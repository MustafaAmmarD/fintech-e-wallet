package com.fintech.ewallet.wallet.application.dto;

import com.fintech.ewallet.wallet.domain.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO after successful wallet transfer.
 */
public record TransferResponse(
        UUID transactionId,
        UUID referenceId,
        UUID fromWalletId,
        UUID toWalletId,
        Currency currency,
        BigDecimal amount,
        String description,
        Instant createdAt) {
}
