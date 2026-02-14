package com.fintech.ewallet.device.application;

import com.fintech.ewallet.device.domain.DeviceFingerprintService;
import com.fintech.ewallet.device.domain.OtpService;
import com.fintech.ewallet.device.domain.TrustedDevice;
import com.fintech.ewallet.device.domain.TrustedDeviceRepository;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.shared.exception.DeviceLimitExceededException;
import com.fintech.ewallet.shared.exception.InvalidOtpException;
import com.fintech.ewallet.shared.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Use case: Verify OTP and register a new trusted device.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyOtpUseCase {

    private final OtpService otpService;
    private final TrustedDeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final DeviceFingerprintService fingerprintService;

    private static final int MAX_DEVICES_PER_USER = 5;

    public void execute(String phoneNumber, String otpCode, String deviceId, HttpServletRequest request) {
        // 1. Verify OTP
        if (!otpService.verifyOtp(phoneNumber, otpCode)) {
            throw new InvalidOtpException();
        }

        // 2. Find user by phone number
        var user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 3. Check device limit
        long deviceCount = deviceRepository.countByUserId(user.getId());
        if (deviceCount >= MAX_DEVICES_PER_USER) {
            throw new DeviceLimitExceededException();
        }

        // 4. Calculate fingerprint
        String fingerprint = fingerprintService.calculateFingerprint(request, deviceId);
        String userAgent = request.getHeader("User-Agent");
        String deviceName = fingerprintService.parseDeviceName(userAgent);
        String ipAddress = request.getRemoteAddr();

        // 5. Check if this is the first device (primary)
        boolean isPrimary = (deviceCount == 0);

        // 6. Create and save device
        TrustedDevice device = TrustedDevice.create(
                user.getId(),
                deviceId,
                fingerprint,
                deviceName,
                userAgent,
                ipAddress,
                isPrimary);

        deviceRepository.save(device);
        log.info("New device registered: {} for user: {}", deviceId, user.getId());
    }
}
