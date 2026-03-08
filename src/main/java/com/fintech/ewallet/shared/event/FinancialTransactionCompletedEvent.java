package com.fintech.ewallet.shared.event;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/**
 * Shared domain event fired when any financial transaction completes.
 * Used for triggering notifications, referrals, etc.
 */
public record FinancialTransactionCompletedEvent(
        String referenceType, // e.g., "TRANSFER", "DEPOSIT", "EXCHANGE"
        UUID referenceId, // ID of the transfer/deposit/exchange
        Set<UUID> participantUserIds, // All users involved (sender, recipient, etc.)
        UUID initiatorId, // Who triggered this? (Useful for "You received from X")
        String initiatorName, // Display name of the initiator
        BigDecimal amount, // Primary amount
        String currency // Primary currency code
) {
}
