package com.fintech.ewallet.admin.domain;

import java.time.Instant;
import java.util.UUID;

public class AdminAction {
    private final UUID id;
    private final UUID adminId;
    private final String actionType;
    private final String targetType;
    private final UUID targetId;
    private final String reason;
    private final Instant createdAt;

    public AdminAction(UUID adminId, String actionType, String targetType, UUID targetId, String reason) {
        this.id = UUID.randomUUID();
        this.adminId = adminId;
        this.actionType = actionType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    public AdminAction(UUID id, UUID adminId, String actionType, String targetType, UUID targetId, String reason,
            Instant createdAt) {
        this.id = id;
        this.adminId = adminId;
        this.actionType = actionType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAdminId() {
        return adminId;
    }

    public String getActionType() {
        return actionType;
    }

    public String getTargetType() {
        return targetType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
