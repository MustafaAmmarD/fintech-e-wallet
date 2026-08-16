package com.fintech.ewallet.wallet.application.dto;

import com.fintech.ewallet.wallet.domain.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request DTO for previewing a P2P transfer.
 */
public record TransferPreviewRequest(
        String recipientAccountNumber,
        String targetPhoneNumber,

        @NotNull(message = "Amount is required") @DecimalMin(value = "0.0001", message = "Amount must be positive") BigDecimal amount,

        @NotNull(message = "Currency is required") Currency currency) {
}
