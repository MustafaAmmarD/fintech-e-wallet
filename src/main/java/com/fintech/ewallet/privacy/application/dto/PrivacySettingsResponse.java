package com.fintech.ewallet.privacy.application.dto;

import java.util.UUID;

public record PrivacySettingsResponse(
        UUID userId,
        boolean showFullName) {
}
