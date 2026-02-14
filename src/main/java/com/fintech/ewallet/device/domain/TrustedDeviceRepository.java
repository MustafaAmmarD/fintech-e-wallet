package com.fintech.ewallet.device.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port (interface) for TrustedDevice persistence.
 */
public interface TrustedDeviceRepository {

    TrustedDevice save(TrustedDevice device);

    Optional<TrustedDevice> findById(UUID id);

    Optional<TrustedDevice> findByUserIdAndDeviceId(UUID userId, String deviceId);

    Optional<TrustedDevice> findByUserIdAndFingerprint(UUID userId, String fingerprint);

    List<TrustedDevice> findAllByUserId(UUID userId);

    long countByUserId(UUID userId);

    void deleteById(UUID id);

    boolean existsByUserIdAndDeviceId(UUID userId, String deviceId);
}
