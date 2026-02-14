package com.fintech.ewallet.device.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO to request OTP for new device verification.
 */
public record RequestOtpRequest(
        @NotBlank(message = "Phone number is required") String phoneNumber) {
}
