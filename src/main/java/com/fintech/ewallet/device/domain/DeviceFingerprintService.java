package com.fintech.ewallet.device.domain;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Service to calculate device fingerprint from HTTP request.
 */
public interface DeviceFingerprintService {

    /**
     * Calculate device fingerprint from HTTP headers.
     *
     * @param request  HTTP request
     * @param deviceId Client-provided device ID
     * @return Fingerprint hash
     */
    String calculateFingerprint(HttpServletRequest request, String deviceId);

    /**
     * Parse User-Agent to generate a friendly device name.
     * <p>
     * Examples:
     * - "Mozilla/5.0 (iPhone...)" → "Safari on iPhone"
     * - "Mozilla/5.0 (Windows NT 10.0...) Chrome/120..." → "Chrome on Windows"
     *
     * @param userAgent User-Agent header
     * @return Friendly device name
     */
    String parseDeviceName(String userAgent);
}
