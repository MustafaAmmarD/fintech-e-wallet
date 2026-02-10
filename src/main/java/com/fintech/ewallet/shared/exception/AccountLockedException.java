package com.fintech.ewallet.shared.exception;

/**
 * Thrown when a locked account attempts to login.
 */
public class AccountLockedException extends DomainException {

    public AccountLockedException() {
        super("ACCOUNT_LOCKED",
                "Account is temporarily locked due to too many failed login attempts. Try again later.");
    }
}
