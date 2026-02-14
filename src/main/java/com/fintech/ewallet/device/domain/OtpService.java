package com.fintech.ewallet.device.domain;

/**
 * Port (interface) for OTP generation and verification.
 */
public interface OtpService {

    /**
     * Generate and store a new OTP for the given phone number.
     *
     * @param phoneNumber Phone number to send OTP to
     * @return The generated OTP (6 digits)
     */
    String generateAndSendOtp(String phoneNumber);

    /**
     * Verify an OTP code.
     *
     * @param phoneNumber Phone number
     * @param otp         OTP code to verify
     * @return true if valid
     */
    boolean verifyOtp(String phoneNumber, String otp);

    /**
     * Check if rate limit exceeded for a phone number.
     *
     * @param phoneNumber Phone number to check
     * @return true if rate limit exceeded (too many requests)
     */
    boolean isRateLimitExceeded(String phoneNumber);
}
