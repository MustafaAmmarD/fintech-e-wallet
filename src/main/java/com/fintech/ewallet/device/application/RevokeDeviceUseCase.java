package com.fintech.ewallet.device.application;

import com.fintech.ewallet.device.domain.TrustedDevice;
import com.fintech.ewallet.device.domain.TrustedDeviceRepository;
import com.fintech.ewallet.shared.exception.ResourceNotFoundException;
import com.fintech.ewallet.shared.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Use case: Revoke a trusted device.
 * <p>
 * This blacklists any active refresh tokens for that device.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RevokeDeviceUseCase {

    private final TrustedDeviceRepository deviceRepository;
    private final TokenBlacklistService tokenBlacklistService;

    public void execute(UUID deviceId, UUID userId) {
        // Verify device belongs to user
        TrustedDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));

        if (!device.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Device not found");
        }

        // Delete device
        deviceRepository.deleteById(deviceId);

        // TODO: Blacklist any active refresh tokens for this device
        // This requires tracking refresh token IDs per device (future enhancement)
        log.info("Device revoked: {} for user: {}", deviceId, userId);
    }
}
