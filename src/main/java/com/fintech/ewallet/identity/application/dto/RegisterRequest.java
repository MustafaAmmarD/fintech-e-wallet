package com.fintech.ewallet.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for user registration.
 */
public record RegisterRequest(

        @NotBlank(message = "Phone number is required") String phoneNumber,

        @NotBlank(message = "Password is required") @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters") String password,

        @NotBlank(message = "Full name is required") @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters") String fullName,

        String email, // Optional

        String language // Optional, defaults to "ar"
) {
}
