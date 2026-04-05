package com.fintech.ewallet.limits.application.dto;

import com.fintech.ewallet.limits.domain.LimitOperationType;
import com.fintech.ewallet.limits.domain.LimitType;
import com.fintech.ewallet.limits.domain.UserTier;
import com.fintech.ewallet.wallet.domain.Currency;

import java.math.BigDecimal;

/**
 * Response DTO for a single transaction limit rule.
 */
public record LimitResponse(
        LimitOperationType operationType,
        Currency currency,
        UserTier userTier,
        LimitType limitType,
        BigDecimal maxAmount,
        Integer windowHours,
        Integer maxCount
) {}
