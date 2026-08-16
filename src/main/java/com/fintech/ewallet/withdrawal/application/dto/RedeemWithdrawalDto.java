package com.fintech.ewallet.withdrawal.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedeemWithdrawalDto(
        @NotBlank(message = "Withdrawal code is required") @Size(min = 6, max = 6, message = "Code must be 6 digits") String withdrawalCode,
        @NotBlank(message = "Idempotency key is required") String idempotencyKey) {
}
