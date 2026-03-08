package com.fintech.ewallet.wallet.domain;

/**
 * Status of a P2P transfer.
 */
public enum TransferStatus {
    COMPLETED, // Successfully executed
    FAILED, // Failed during execution
    REVERSED // Reversed after completion (compensating entries created)
}
