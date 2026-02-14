package com.fintech.ewallet.device.application;

import com.fintech.ewallet.device.application.dto.DeviceListResponse;
import com.fintech.ewallet.device.domain.TrustedDevice;
import com.fintech.ewallet.device.domain.TrustedDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Use case: List all trusted devices for a user.
 */
@Service
@RequiredArgsConstructor
public class ListDevicesUseCase {

    private final TrustedDeviceRepository deviceRepository;

    public DeviceListResponse execute(UUID userId) {
        List<TrustedDevice> devices = deviceRepository.findAllByUserId(userId);

        List<DeviceListResponse.DeviceInfo> deviceInfos = devices.stream()
                .map(device -> new DeviceListResponse.DeviceInfo(
                        device.getId(),
                        device.getDeviceId(),
                        device.getDeviceName(),
                        device.isPrimary(),
                        device.getLastUsedAt(),
                        device.getCreatedAt()))
                .toList();

        return new DeviceListResponse(deviceInfos);
    }
}
