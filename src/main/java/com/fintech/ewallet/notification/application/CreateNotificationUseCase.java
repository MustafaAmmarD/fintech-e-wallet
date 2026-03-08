package com.fintech.ewallet.notification.application;

import com.fintech.ewallet.notification.domain.Notification;
import com.fintech.ewallet.notification.domain.NotificationRepository;
import com.fintech.ewallet.notification.domain.NotificationType;
import com.fintech.ewallet.notification.domain.NotificationSender;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class CreateNotificationUseCase implements NotificationSender {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(
            UUID userId,
            NotificationType type,
            String title,
            String message,
            String referenceType,
            UUID referenceId) {

        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("Notification type is required");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Notification title is required");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Notification message is required");
        }

        Notification notification = new Notification(
                userId,
                type,
                title.trim(),
                message.trim(),
                normalizeNullable(referenceType),
                referenceId);

        notificationRepository.save(notification);
    }

    private String normalizeNullable(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
