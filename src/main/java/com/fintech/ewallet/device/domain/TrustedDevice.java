package com.fintech.ewallet.device.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Trusted Device domain entity.
 * <p>
 * Represents a device that has been verified and trusted for a user.
 * Each device gets its own refresh token.
 */
public class TrustedDevice {

    private UUID id;
    private UUID userId;
    private String deviceId; // Client-generated UUID
    private String fingerprint; // Server-calculated hash
    private String deviceName; // Auto-generated from User-Agent
    private String userAgent;
    private String lastIpAddress;
    private boolean isPrimary; // First device registered
    private Instant lastUsedAt;
    private Instant createdAt;

    // Public no-arg constructor for JPA/MapStruct
    public TrustedDevice() {
    }

    /**
     * Factory method: Create a new trusted device.
     */
    public static TrustedDevice create(
            UUID userId,
            String deviceId,
            String fingerprint,
            String deviceName,
            String userAgent,
            String ipAddress,
            boolean isPrimary) {

        TrustedDevice device = new TrustedDevice();
        device.id = UUID.randomUUID();
        device.userId = userId;
        device.deviceId = deviceId;
        device.fingerprint = fingerprint;
        device.deviceName = deviceName;
        device.userAgent = userAgent;
        device.lastIpAddress = ipAddress;
        device.isPrimary = isPrimary;
        device.lastUsedAt = Instant.now();
        device.createdAt = Instant.now();
        return device;
    }

    /**
     * Update last used timestamp and IP address.
     */
    public void updateLastUsed(String ipAddress) {
        this.lastUsedAt = Instant.now();
        this.lastIpAddress = ipAddress;
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getLastIpAddress() {
        return lastIpAddress;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // Setters for JPA/Mappers
    public void setId(UUID id) {
        this.id = id;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setLastIpAddress(String lastIpAddress) {
        this.lastIpAddress = lastIpAddress;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
