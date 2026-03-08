package com.fintech.ewallet.device.infrastructure.persistence;

import com.fintech.ewallet.device.domain.TrustedDevice;
import com.fintech.ewallet.device.domain.TrustedDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter implementing TrustedDeviceRepository using JPA.
 */
@Component
@RequiredArgsConstructor
public class TrustedDeviceRepositoryAdapter implements TrustedDeviceRepository {

    private final TrustedDeviceJpaRepository jpaRepository;

    @Override
    public TrustedDevice save(TrustedDevice device) {
        TrustedDeviceJpaEntity entity = toEntity(device);
        TrustedDeviceJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<TrustedDevice> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<TrustedDevice> findByUserIdAndDeviceId(UUID userId, String deviceId) {
        return jpaRepository.findByUserIdAndDeviceId(userId, deviceId).map(this::toDomain);
    }

    @Override
    public Optional<TrustedDevice> findByUserIdAndFingerprint(UUID userId, String fingerprint) {
        return jpaRepository.findByUserIdAndFingerprint(userId, fingerprint).map(this::toDomain);
    }

    @Override
    public List<TrustedDevice> findAllByUserId(UUID userId) {
        return jpaRepository.findAllByUserId(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByUserId(UUID userId) {
        return jpaRepository.countByUserId(userId);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByUserIdAndDeviceId(UUID userId, String deviceId) {
        return jpaRepository.existsByUserIdAndDeviceId(userId, deviceId);
    }

    private TrustedDevice toDomain(TrustedDeviceJpaEntity entity) {
        TrustedDevice trustedDevice = new TrustedDevice();
        trustedDevice.setId(entity.getId());
        trustedDevice.setUserId(entity.getUserId());
        trustedDevice.setDeviceId(entity.getDeviceId());
        trustedDevice.setFingerprint(entity.getFingerprint());
        trustedDevice.setDeviceName(entity.getDeviceName());
        trustedDevice.setUserAgent(entity.getUserAgent());
        trustedDevice.setLastIpAddress(entity.getLastIpAddress());
        trustedDevice.setPrimary(entity.isPrimary());
        trustedDevice.setLastUsedAt(entity.getLastUsedAt());
        trustedDevice.setCreatedAt(entity.getCreatedAt());
        return trustedDevice;
    }

    private TrustedDeviceJpaEntity toEntity(TrustedDevice domain) {
        TrustedDeviceJpaEntity entity = new TrustedDeviceJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setDeviceId(domain.getDeviceId());
        entity.setFingerprint(domain.getFingerprint());
        entity.setDeviceName(domain.getDeviceName());
        entity.setUserAgent(domain.getUserAgent());
        entity.setLastIpAddress(domain.getLastIpAddress());
        entity.setPrimary(domain.isPrimary());
        entity.setLastUsedAt(domain.getLastUsedAt());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
