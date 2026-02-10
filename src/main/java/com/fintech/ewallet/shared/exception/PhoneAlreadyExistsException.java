package com.fintech.ewallet.shared.exception;

/**
 * Thrown when a user tries to register with a phone number that already exists.
 */
public class PhoneAlreadyExistsException extends DomainException {

    public PhoneAlreadyExistsException(String phoneNumber) {
        super("PHONE_ALREADY_EXISTS",
                "A user with phone number " + phoneNumber + " already exists");
    }
}
