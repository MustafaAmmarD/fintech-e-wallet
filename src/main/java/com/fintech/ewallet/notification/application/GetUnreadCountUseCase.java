package com.fintech.ewallet.notification.application;

import com.fintech.ewallet.notification.application.dto.UnreadCountResponse;
import com.fintech.ewallet.notification.domain.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetUnreadCountUseCase {

    private final NotificationRepository notificationRepository;

    public UnreadCountResponse execute(UUID userId) {
        long unreadCount = notificationRepository.countUnreadByUserId(userId);
        return new UnreadCountResponse(unreadCount);
    }
}
