package com.fintech.ewallet.shared.exception;

/**
 * Thrown when login credentials are invalid (wrong phone or password).
 * Intentionally vague message to avoid leaking whether a phone number is
 * registered.
 */
public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "Invalid phone number or password");
    }
}
