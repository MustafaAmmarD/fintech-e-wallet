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
    private final TrustedDeviceMapper mapper;

    @Override
    public TrustedDevice save(TrustedDevice device) {
        TrustedDeviceJpaEntity entity = mapper.toEntity(device);
        TrustedDeviceJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<TrustedDevice> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<TrustedDevice> findByUserIdAndDeviceId(UUID userId, String deviceId) {
        return jpaRepository.findByUserIdAndDeviceId(userId, deviceId).map(mapper::toDomain);
    }

    @Override
    public Optional<TrustedDevice> findByUserIdAndFingerprint(UUID userId, String fingerprint) {
        return jpaRepository.findByUserIdAndFingerprint(userId, fingerprint).map(mapper::toDomain);
    }

    @Override
    public List<TrustedDevice> findAllByUserId(UUID userId) {
        return jpaRepository.findAllByUserId(userId).stream()
                .map(mapper::toDomain)
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
}
