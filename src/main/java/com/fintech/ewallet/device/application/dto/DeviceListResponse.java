package com.fintech.ewallet.device.application.dto;

/**
 * Response DTO listing a user's trusted devices.
 */
public record DeviceListResponse(
        java.util.List<DeviceInfo> devices) {
    public record DeviceInfo(
            java.util.UUID id,
            String deviceId,
            String deviceName,
            boolean isPrimary,
            java.time.Instant lastUsedAt,
            java.time.Instant createdAt) {
    }
}
