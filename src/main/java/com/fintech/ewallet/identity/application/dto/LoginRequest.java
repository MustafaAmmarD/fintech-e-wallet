package com.fintech.ewallet.identity.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for user login.
 * Phase 1.4: Includes device information for binding.
 */
public record LoginRequest(

                @NotBlank(message = "Phone number is required") String phoneNumber,

                @NotBlank(message = "Password is required") String password,

                @NotBlank(message = "Device ID is required") String deviceId,

                String deviceName // Optional, will auto-generate from User-Agent if not provided
) {
}
