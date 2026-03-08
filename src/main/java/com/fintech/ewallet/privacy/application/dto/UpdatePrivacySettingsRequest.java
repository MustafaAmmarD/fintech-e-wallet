package com.fintech.ewallet.privacy.application.dto;

import jakarta.validation.constraints.NotNull;

public record UpdatePrivacySettingsRequest(
        @NotNull(message = "showFullName is required")
        Boolean showFullName) {
}
