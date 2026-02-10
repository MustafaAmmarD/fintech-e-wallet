package com.fintech.ewallet.identity.domain;

/**
 * Account status for a user.
 */
public enum AccountStatus {
    ACTIVE, // Normal operating state
    SUSPENDED, // Temporarily locked (e.g., suspicious activity)
    CLOSED // Permanently closed / soft-deleted
}
