package com.fintech.ewallet.notification.infrastructure.persistence;

import com.fintech.ewallet.notification.domain.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationJpaEntity toEntity(Notification domain) {
        NotificationJpaEntity entity = new NotificationJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setType(domain.getType());
        entity.setTitle(domain.getTitle());
        entity.setMessage(domain.getMessage());
        entity.setReferenceType(domain.getReferenceType());
        entity.setReferenceId(domain.getReferenceId());
        entity.setRead(domain.isRead());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    public Notification toDomain(NotificationJpaEntity entity) {
        return new Notification(
                entity.getId(),
                entity.getUserId(),
                entity.getType(),
                entity.getTitle(),
                entity.getMessage(),
                entity.getReferenceType(),
                entity.getReferenceId(),
                entity.isRead(),
                entity.getCreatedAt());
    }
}
