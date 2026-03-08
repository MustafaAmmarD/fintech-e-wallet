package com.fintech.ewallet.withdrawal.application.dto;

import com.fintech.ewallet.wallet.domain.Currency;
import com.fintech.ewallet.withdrawal.domain.WithdrawalStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AgentWithdrawResponse(
        UUID withdrawalId,
        String referenceNo,
        String userDisplayName,
        BigDecimal amount,
        Currency currency,
        WithdrawalStatus status,
        Instant createdAt) {
}
