package com.fintech.ewallet.shared.exception;

/**
 * Thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }
}
