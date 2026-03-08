package com.fintech.ewallet.deposit.application.dto;

import com.fintech.ewallet.deposit.domain.DepositStatus;
import com.fintech.ewallet.wallet.domain.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AgentDepositResponse(
        UUID depositId,
        String referenceNo,
        String recipientDisplayName,
        BigDecimal amount,
        Currency currency,
        DepositStatus status,
        Instant createdAt) {
}
