package com.fintech.ewallet.notification.application;

import com.fintech.ewallet.notification.application.dto.NotificationResponse;
import com.fintech.ewallet.notification.domain.Notification;
import com.fintech.ewallet.notification.domain.NotificationRepository;
import com.fintech.ewallet.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarkReadUseCase {

    private final NotificationRepository notificationRepository;

    @Transactional
    public NotificationResponse execute(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Notification not found");
        }

        if (!notification.isRead()) {
            notification.markRead();
            notification = notificationRepository.save(notification);
        }

        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
