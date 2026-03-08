package com.fintech.ewallet.fee.application.dto;

import com.fintech.ewallet.fee.domain.FeeOperation;
import com.fintech.ewallet.fee.domain.FeeType;
import com.fintech.ewallet.wallet.domain.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FeeRuleResponse(
        UUID id,
        FeeOperation operationType,
        Currency currency,
        FeeType feeType,
        BigDecimal rate,
        BigDecimal flatAmount,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        BigDecimal minFee,
        BigDecimal maxFee,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
