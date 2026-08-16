package com.fintech.ewallet.wallet.application.dto;

import com.fintech.ewallet.wallet.domain.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request DTO for executing a confirmed P2P transfer.
 */
public record ExecuteTransferRequest(
        String recipientAccountNumber,
        String targetPhoneNumber,

        @NotNull(message = "Amount is required") @DecimalMin(value = "0.0001", message = "Amount must be positive") BigDecimal amount,

        @NotNull(message = "Currency is required") Currency currency,

        @Size(max = 500, message = "Description must be at most 500 characters") String description) {
}
