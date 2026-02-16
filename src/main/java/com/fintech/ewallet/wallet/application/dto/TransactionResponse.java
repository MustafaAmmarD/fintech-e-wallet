package com.fintech.ewallet.wallet.application.dto;

import com.fintech.ewallet.wallet.domain.EntryType;
import com.fintech.ewallet.wallet.domain.ReferenceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID transactionId,
        EntryType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        ReferenceType referenceType,
        String description,
        Instant createdAt) {
}
