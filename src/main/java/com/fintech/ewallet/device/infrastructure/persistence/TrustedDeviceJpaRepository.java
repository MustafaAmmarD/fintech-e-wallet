package com.fintech.ewallet.device.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for TrustedDevice.
 */
@Repository
public interface TrustedDeviceJpaRepository extends JpaRepository<TrustedDeviceJpaEntity, UUID> {

    Optional<TrustedDeviceJpaEntity> findByUserIdAndDeviceId(UUID userId, String deviceId);

    Optional<TrustedDeviceJpaEntity> findByUserIdAndFingerprint(UUID userId, String fingerprint);

    List<TrustedDeviceJpaEntity> findAllByUserId(UUID userId);

    long countByUserId(UUID userId);

    boolean existsByUserIdAndDeviceId(UUID userId, String deviceId);
}
