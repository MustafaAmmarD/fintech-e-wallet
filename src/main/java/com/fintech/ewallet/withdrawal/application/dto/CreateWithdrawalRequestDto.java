package com.fintech.ewallet.withdrawal.application.dto;

import com.fintech.ewallet.wallet.domain.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateWithdrawalRequestDto(
        @NotNull(message = "Wallet ID is required") UUID walletId,
        @NotNull(message = "Amount is required") @DecimalMin(value = "0.0001", message = "Amount must be positive") BigDecimal amount,
        @NotNull(message = "Currency is required") Currency currency) {
}
