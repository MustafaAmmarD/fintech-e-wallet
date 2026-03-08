package com.fintech.ewallet.notification.application;

import com.fintech.ewallet.notification.application.dto.UnreadCountResponse;
import com.fintech.ewallet.notification.domain.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarkAllReadUseCase {

    private final NotificationRepository notificationRepository;

    @Transactional
    public UnreadCountResponse execute(UUID userId) {
        notificationRepository.markAllAsReadByUserId(userId);
        return new UnreadCountResponse(notificationRepository.countUnreadByUserId(userId));
    }
}
