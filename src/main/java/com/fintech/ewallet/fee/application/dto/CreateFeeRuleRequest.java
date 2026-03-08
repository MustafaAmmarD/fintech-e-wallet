package com.fintech.ewallet.fee.application.dto;

import com.fintech.ewallet.fee.domain.FeeOperation;
import com.fintech.ewallet.fee.domain.FeeType;
import com.fintech.ewallet.wallet.domain.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateFeeRuleRequest(
        @NotNull(message = "Operation type is required") FeeOperation operationType,
        @NotNull(message = "Currency is required") Currency currency,
        @NotNull(message = "Fee type is required") FeeType feeType,
        BigDecimal rate,
        BigDecimal flatAmount,
        @NotNull(message = "Min amount is required") @DecimalMin(value = "0.0000", message = "Min amount must be >= 0") BigDecimal minAmount,
        BigDecimal maxAmount,
        @NotNull(message = "Min fee is required") @DecimalMin(value = "0.0000", message = "Min fee must be >= 0") BigDecimal minFee,
        BigDecimal maxFee,
        boolean replaceExisting) {
}
