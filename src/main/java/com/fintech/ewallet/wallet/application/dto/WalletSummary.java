package com.fintech.ewallet.wallet.application.dto;

import com.fintech.ewallet.wallet.domain.Currency;
import com.fintech.ewallet.wallet.domain.WalletStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletSummary(
        UUID walletId,
        Currency currency,
        BigDecimal balance,
        WalletStatus status) {
}
