package com.fintech.ewallet.device.infrastructure.security;

import com.fintech.ewallet.device.domain.DeviceFingerprintService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Service to calculate device fingerprint from HTTP request headers.
 */
@Slf4j
@Service
public class DeviceFingerprintServiceImpl implements DeviceFingerprintService {

    @Override
    public String calculateFingerprint(HttpServletRequest request, String deviceId) {
        String userAgent = request.getHeader("User-Agent");
        String acceptLanguage = request.getHeader("Accept-Language");

        // Create fingerprint from: deviceId + User-Agent + Accept-Language
        String rawFingerprint = deviceId +
                "|" + (userAgent != null ? userAgent : "") +
                "|" + (acceptLanguage != null ? acceptLanguage : "");

        return hashSHA256(rawFingerprint);
    }

    @Override
    public String parseDeviceName(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown Device";
        }

        // Detect browser
        String browser = "Unknown Browser";
        if (userAgent.contains("Chrome") && !userAgent.contains("Edg")) {
            browser = "Chrome";
        } else if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) {
            browser = "Safari";
        } else if (userAgent.contains("Firefox")) {
            browser = "Firefox";
        } else if (userAgent.contains("Edg")) {
            browser = "Edge";
        }

        // Detect platform
        String platform = "Unknown OS";
        if (userAgent.contains("iPhone")) {
            platform = "iPhone";
        } else if (userAgent.contains("iPad")) {
            platform = "iPad";
        } else if (userAgent.contains("Android")) {
            platform = "Android";
        } else if (userAgent.contains("Windows NT")) {
            platform = "Windows";
        } else if (userAgent.contains("Macintosh")) {
            platform = "macOS";
        } else if (userAgent.contains("Linux")) {
            platform = "Linux";
        }

        return browser + " on " + platform;
    }

    /**
     * SHA-256 hash function.
     */
    private String hashSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found", e);
            throw new RuntimeException("Failed to calculate fingerprint", e);
        }
    }
}
