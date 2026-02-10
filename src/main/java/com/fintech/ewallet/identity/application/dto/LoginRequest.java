package com.fintech.ewallet.identity.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for user login.
 */
public record LoginRequest(

        @NotBlank(message = "Phone number is required") String phoneNumber,

        @NotBlank(message = "Password is required") String password) {
}
