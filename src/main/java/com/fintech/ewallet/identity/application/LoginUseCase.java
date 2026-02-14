package com.fintech.ewallet.identity.application;

import com.fintech.ewallet.device.domain.DeviceFingerprintService;
import com.fintech.ewallet.device.domain.TrustedDevice;
import com.fintech.ewallet.device.domain.TrustedDeviceRepository;
import com.fintech.ewallet.identity.application.dto.LoginRequest;
import com.fintech.ewallet.identity.application.dto.LoginResponse;
import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.identity.infrastructure.security.JwtTokenProvider;
import com.fintech.ewallet.shared.exception.AccountLockedException;
import com.fintech.ewallet.shared.exception.DeviceLimitExceededException;
import com.fintech.ewallet.shared.exception.InvalidCredentialsException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case: Login with phone + password + device binding.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TrustedDeviceRepository deviceRepository;
    private final DeviceFingerprintService fingerprintService;

    private static final int MAX_DEVICES_PER_USER = 5;

    @Transactional
    public LoginResponse execute(LoginRequest request, HttpServletRequest httpRequest) {
        log.debug("Login attempt for phone: {}", maskPhone(request.phoneNumber()));

        // 1. Find user by phone
        User user = userRepository.findByPhoneNumber(request.phoneNumber())
                .orElseThrow(InvalidCredentialsException::new);

        // 2. Check if account is locked
        if (user.isLocked()) {
            log.warn("Login blocked — account locked for user: {}", user.getId());
            throw new AccountLockedException();
        }

        // 3. Check if account is active
        if (!user.isActive()) {
            log.warn("Login blocked — account not active for user: {}", user.getId());
            throw new InvalidCredentialsException();
        }

        // 4. Verify password
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.recordFailedLogin();
            userRepository.save(user);
            log.warn("Failed login attempt #{} for user: {}", user.getFailedLoginAttempts(), user.getId());
            throw new InvalidCredentialsException();
        }

        // 5. Successful login — reset failed attempts
        user.recordSuccessfulLogin();
        userRepository.save(user);

        // 6. Register or update device
        TrustedDevice device = registerOrUpdateDevice(user, request, httpRequest);

        // 7. Generate JWT tokens with deviceId
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user, device.getDeviceId());
        long expiresIn = jwtTokenProvider.getAccessTokenExpirationMs();

        log.info("User logged in successfully: {} from device: {}", user.getId(), device.getDeviceId());

        // 8. Build response
        return new LoginResponse(
                accessToken,
                refreshToken,
                expiresIn,
                new LoginResponse.UserInfo(
                        user.getId(),
                        user.getFullName(),
                        user.getPhoneNumber(),
                        user.getKycStatus().name()));
    }

    /**
     * Register or update trusted device.
     */
    private TrustedDevice registerOrUpdateDevice(User user, LoginRequest request, HttpServletRequest httpRequest) {
        String deviceId = request.deviceId();
        String fingerprint = fingerprintService.calculateFingerprint(httpRequest, deviceId);

        // Check if device already exists
        return deviceRepository.findByUserIdAndDeviceId(user.getId(), deviceId)
                .map(existingDevice -> {
                    // Update last used info
                    existingDevice.updateLastUsed(httpRequest.getRemoteAddr());
                    return deviceRepository.save(existingDevice);
                })
                .orElseGet(() -> {
                    // New device — check limit
                    long deviceCount = deviceRepository.countByUserId(user.getId());
                    if (deviceCount >= MAX_DEVICES_PER_USER) {
                        throw new DeviceLimitExceededException();
                    }

                    // Create new device
                    String userAgent = httpRequest.getHeader("User-Agent");
                    String deviceName = request.deviceName() != null
                            ? request.deviceName()
                            : fingerprintService.parseDeviceName(userAgent);
                    String ipAddress = httpRequest.getRemoteAddr();
                    boolean isPrimary = (deviceCount == 0);

                    TrustedDevice newDevice = TrustedDevice.create(
                            user.getId(),
                            deviceId,
                            fingerprint,
                            deviceName,
                            userAgent,
                            ipAddress,
                            isPrimary);

                    return deviceRepository.save(newDevice);
                });
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6)
            return "***";
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }
}
