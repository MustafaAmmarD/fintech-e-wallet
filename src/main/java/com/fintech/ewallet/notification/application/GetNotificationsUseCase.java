package com.fintech.ewallet.notification.application;

import com.fintech.ewallet.notification.application.dto.NotificationResponse;
import com.fintech.ewallet.notification.domain.Notification;
import com.fintech.ewallet.notification.domain.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetNotificationsUseCase {

    private final NotificationRepository notificationRepository;

    public List<NotificationResponse> execute(UUID userId, int limit) {
        int clampedLimit = Math.min(Math.max(limit, 1), 50);

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, clampedLimit).stream()
                .map(this::toResponse)
                .toList();
    }

    private NotificationResponse toResponse(Notification notification) {
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
