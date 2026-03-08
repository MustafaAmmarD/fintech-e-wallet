package com.fintech.ewallet.notification.infrastructure.persistence;

import com.fintech.ewallet.notification.domain.Notification;
import com.fintech.ewallet.notification.domain.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final NotificationJpaRepository notificationJpaRepository;
    private final NotificationMapper notificationMapper;

    @Override
    public Notification save(Notification notification) {
        NotificationJpaEntity saved = notificationJpaRepository.save(notificationMapper.toEntity(notification));
        return notificationMapper.toDomain(saved);
    }

    @Override
    public Optional<Notification> findById(UUID notificationId) {
        return notificationJpaRepository.findById(notificationId)
                .map(notificationMapper::toDomain);
    }

    @Override
    public List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, int limit) {
        return notificationJpaRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit))
                .stream()
                .map(notificationMapper::toDomain)
                .toList();
    }

    @Override
    public long countUnreadByUserId(UUID userId) {
        return notificationJpaRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    public int markAllAsReadByUserId(UUID userId) {
        return notificationJpaRepository.markAllAsReadByUserId(userId);
    }
}
