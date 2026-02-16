package com.fintech.ewallet.wallet.domain;

import java.util.UUID;

/**
 * Constants for system wallet IDs.
 * These are special wallets owned by the platform (not users).
 */
public final class SystemWallets {

    // Liquidity Wallets (Float Management)
    public static final UUID LIQUIDITY_YER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID LIQUIDITY_SAR = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID LIQUIDITY_USD = UUID.fromString("00000000-0000-0000-0000-000000000003");

    // Fee Collection Wallets (Revenue)
    public static final UUID FEES_YER = UUID.fromString("00000000-0000-0000-0000-000000000011");
    public static final UUID FEES_SAR = UUID.fromString("00000000-0000-0000-0000-000000000012");
    public static final UUID FEES_USD = UUID.fromString("00000000-0000-0000-0000-000000000013");

    private SystemWallets() {
        // Prevent instantiation
    }

    /**
     * Get liquidity wallet ID for a given currency.
     */
    public static UUID getLiquidityWallet(Currency currency) {
        return switch (currency) {
            case YER -> LIQUIDITY_YER;
            case SAR -> LIQUIDITY_SAR;
            case USD -> LIQUIDITY_USD;
        };
    }

    /**
     * Get fee wallet ID for a given currency.
     */
    public static UUID getFeeWallet(Currency currency) {
        return switch (currency) {
            case YER -> FEES_YER;
            case SAR -> FEES_SAR;
            case USD -> FEES_USD;
        };
    }

    /**
     * Check if a wallet ID is a system wallet.
     */
    public static boolean isSystemWallet(UUID walletId) {
        return walletId.equals(LIQUIDITY_YER) || walletId.equals(LIQUIDITY_SAR) || walletId.equals(LIQUIDITY_USD)
                || walletId.equals(FEES_YER) || walletId.equals(FEES_SAR) || walletId.equals(FEES_USD);
    }
}
