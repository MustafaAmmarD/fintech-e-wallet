package com.fintech.ewallet.shared.exception;

/**
 * Thrown when a login attempt comes from an untrusted device.
 */
public class OtpVerificationRequiredException extends DomainException {

    public OtpVerificationRequiredException() {
        super("OTP_VERIFICATION_REQUIRED",
                "This device is not trusted. Please verify OTP before login.");
    }
}
