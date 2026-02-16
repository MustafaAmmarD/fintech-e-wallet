package com.fintech.ewallet.wallet.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ReconciliationResult(
        UUID walletId,
        BigDecimal cachedBalance,
        BigDecimal ledgerBalance,
        boolean matches,
        BigDecimal discrepancy) {
}
