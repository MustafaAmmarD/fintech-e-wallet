package com.fintech.ewallet.wallet.domain;

/**
 * Status of a P2P transfer.
 */
public enum TransferStatus {
    COMPLETED, // Successfully executed
    FAILED, // Failed during execution
    REVERSED, // Reversed after completion (compensating entries created)
    PENDING, // Waiting to be received/claimed
    UNCLAIMED, // Has been waiting for a long time
    CANCELLED // Cancelled before being claimed
}
