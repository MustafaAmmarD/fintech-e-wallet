package com.fintech.ewallet.wallet.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for wallet-to-wallet transfer.
 */
public record TransferRequest(
        @NotNull(message = "Source wallet ID is required") UUID fromWalletId,
        @NotNull(message = "Destination wallet ID is required") UUID toWalletId,
        @NotNull(message = "Amount is required") @DecimalMin(value = "0.0001", message = "Amount must be positive") BigDecimal amount,
        @Size(max = 200, message = "Description must be at most 200 characters") String description) {
}
