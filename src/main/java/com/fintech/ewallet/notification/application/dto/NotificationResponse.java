package com.fintech.ewallet.notification.application.dto;

import com.fintech.ewallet.notification.domain.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String message,
        String referenceType,
        UUID referenceId,
        boolean isRead,
        Instant createdAt) {
}
