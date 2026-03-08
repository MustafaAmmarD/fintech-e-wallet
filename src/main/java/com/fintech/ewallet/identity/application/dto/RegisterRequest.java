package com.fintech.ewallet.identity.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for user registration.
 */
public record RegisterRequest(

        @NotBlank(message = "Phone number is required") @Pattern(regexp = "^\\+967\\d{9}$", message = "Phone number must be Yemen format: +967XXXXXXXXX") String phoneNumber,

        @NotBlank(message = "Password is required") @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters") String password,

        @NotBlank(message = "Full name is required") @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters") String fullName,

        @Email(message = "Email format is invalid") String email, // Optional

        String language, // Optional, defaults to "ar"

        @Size(max = 20, message = "Referral code must not exceed 20 characters") String referralCode // Optional
) {
}
