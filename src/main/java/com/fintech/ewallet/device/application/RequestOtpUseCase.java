package com.fintech.ewallet.device.application;

import com.fintech.ewallet.device.domain.OtpService;
import com.fintech.ewallet.shared.exception.OtpRateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Use case: Request OTP for new device verification.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequestOtpUseCase {

    private final OtpService otpService;

    public void execute(String phoneNumber) {
        // Check rate limit
        if (otpService.isRateLimitExceeded(phoneNumber)) {
            throw new OtpRateLimitExceededException();
        }

        // Generate and send OTP
        otpService.generateAndSendOtp(phoneNumber);
        log.info("OTP requested for phone: {}", phoneNumber);
    }
}
