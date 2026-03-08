package com.fintech.ewallet.notification.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {

    Notification save(Notification notification);

    Optional<Notification> findById(UUID notificationId);

    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, int limit);

    long countUnreadByUserId(UUID userId);

    int markAllAsReadByUserId(UUID userId);
}
