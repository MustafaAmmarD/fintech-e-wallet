package com.fintech.ewallet.wallet.domain;

public enum WalletStatus {
    ACTIVE,
    FROZEN, // Verification needed or suspicious activity
    CLOSED // Account closed
}
