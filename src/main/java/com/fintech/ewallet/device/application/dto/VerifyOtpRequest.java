package com.fintech.ewallet.device.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO to verify OTP and bind a new device.
 */
public record VerifyOtpRequest(
        @NotBlank(message = "Phone number is required") String phoneNumber,

        @NotBlank(message = "OTP code is required") @Pattern(regexp = "\\d{6}", message = "OTP must be 6 digits") String otpCode,

        @NotBlank(message = "Device ID is required") String deviceId) {
}
