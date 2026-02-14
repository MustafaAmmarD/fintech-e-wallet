package com.fintech.ewallet.device.api;

import com.fintech.ewallet.device.application.ListDevicesUseCase;
import com.fintech.ewallet.device.application.RequestOtpUseCase;
import com.fintech.ewallet.device.application.RevokeDeviceUseCase;
import com.fintech.ewallet.device.application.VerifyOtpUseCase;
import com.fintech.ewallet.device.application.dto.DeviceListResponse;
import com.fintech.ewallet.device.application.dto.RequestOtpRequest;
import com.fintech.ewallet.device.application.dto.VerifyOtpRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Device management endpoints.
 */
@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final ListDevicesUseCase listDevicesUseCase;
    private final RevokeDeviceUseCase revokeDeviceUseCase;
    private final RequestOtpUseCase requestOtpUseCase;
    private final VerifyOtpUseCase verifyOtpUseCase;

    /**
     * List all trusted devices for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<DeviceListResponse> listDevices(@AuthenticationPrincipal UUID userId) {
        DeviceListResponse response = listDevicesUseCase.execute(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Revoke (delete) a trusted device.
     */
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> revokeDevice(
            @PathVariable UUID deviceId,
            @AuthenticationPrincipal UUID userId) {

        revokeDeviceUseCase.execute(deviceId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Request OTP for new device verification (public endpoint).
     */
    @PostMapping("/request-otp")
    public ResponseEntity<Void> requestOtp(@Valid @RequestBody RequestOtpRequest request) {
        requestOtpUseCase.execute(request.phoneNumber());
        return ResponseEntity.ok().build();
    }

    /**
     * Verify OTP and register a new device (public endpoint).
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<Void> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest) {

        verifyOtpUseCase.execute(
                request.phoneNumber(),
                request.otpCode(),
                request.deviceId(),
                httpRequest);
        return ResponseEntity.ok().build();
    }
}
