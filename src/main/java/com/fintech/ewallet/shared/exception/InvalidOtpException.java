package com.fintech.ewallet.shared.exception;

/**
 * Thrown when OTP is invalid or expired.
 */
public class InvalidOtpException extends DomainException {

    public InvalidOtpException() {
        super("INVALID_OTP",
                "The OTP code is invalid or has expired. Please request a new one.");
    }
}
