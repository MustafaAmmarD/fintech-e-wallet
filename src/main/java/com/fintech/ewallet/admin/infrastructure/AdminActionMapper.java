package com.fintech.ewallet.admin.infrastructure;

import com.fintech.ewallet.admin.domain.AdminAction;
import org.springframework.stereotype.Component;

@Component
public class AdminActionMapper {

    public AdminActionJpaEntity toEntity(AdminAction domain) {
        if (domain == null) {
            return null;
        }

        return AdminActionJpaEntity.builder()
                .id(domain.getId())
                .adminId(domain.getAdminId())
                .actionType(domain.getActionType())
                .targetType(domain.getTargetType())
                .targetId(domain.getTargetId())
                .reason(domain.getReason())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public AdminAction toDomain(AdminActionJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return new AdminAction(
                entity.getId(),
                entity.getAdminId(),
                entity.getActionType(),
                entity.getTargetType(),
                entity.getTargetId(),
                entity.getReason(),
                entity.getCreatedAt());
    }
}
