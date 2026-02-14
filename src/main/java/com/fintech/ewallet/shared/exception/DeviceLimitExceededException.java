package com.fintech.ewallet.shared.exception;

/**
 * Thrown when device limit is exceeded (max 5 devices per user).
 */
public class DeviceLimitExceededException extends DomainException {

    public DeviceLimitExceededException() {
        super("DEVICE_LIMIT_EXCEEDED",
                "Maximum number of trusted devices reached. Please revoke a device first.");
    }
}
