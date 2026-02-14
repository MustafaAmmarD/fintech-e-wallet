package com.fintech.ewallet.device.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for trusted_devices table.
 */
@Data
@Entity
@Table(name = "trusted_devices", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id",
        "device_id" }), indexes = {
                @Index(name = "idx_trusted_devices_user", columnList = "user_id"),
                @Index(name = "idx_trusted_devices_fingerprint", columnList = "fingerprint")
        })
public class TrustedDeviceJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "fingerprint", nullable = false)
    private String fingerprint;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "last_ip_address", length = 45)
    private String lastIpAddress;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
