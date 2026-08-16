package com.fintech.ewallet.withdrawal.application.dto;

import com.fintech.ewallet.wallet.domain.Currency;
import com.fintech.ewallet.withdrawal.domain.WithdrawalRequestStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WithdrawalRequestResponse(
        UUID id,
        String withdrawalCode,
        BigDecimal amount,
        Currency currency,
        WithdrawalRequestStatus status,
        Instant expiresAt,
        Instant createdAt) {
}
