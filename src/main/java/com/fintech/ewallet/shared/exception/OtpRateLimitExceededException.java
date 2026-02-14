package com.fintech.ewallet.shared.exception;

/**
 * Thrown when OTP rate limit is exceeded.
 */
public class OtpRateLimitExceededException extends DomainException {

    public OtpRateLimitExceededException() {
        super("OTP_RATE_LIMIT_EXCEEDED",
                "Too many OTP requests. Please try again later.");
    }
}
