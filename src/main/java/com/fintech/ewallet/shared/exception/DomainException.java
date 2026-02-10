package com.fintech.ewallet.shared.exception;

/**
 * Base domain exception for all business-rule violations.
 * Subclasses represent specific error conditions.
 */
public abstract class DomainException extends RuntimeException {

    private final String errorCode;

    protected DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
