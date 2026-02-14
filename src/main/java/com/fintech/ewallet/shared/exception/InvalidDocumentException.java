package com.fintech.ewallet.shared.exception;

/**
 * Thrown when an uploaded document is invalid.
 */
public class InvalidDocumentException extends DomainException {

    public InvalidDocumentException(String message) {
        super("INVALID_DOCUMENT", message);
    }
}
