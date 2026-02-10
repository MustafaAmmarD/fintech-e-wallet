package com.fintech.ewallet.shared.util;

import java.util.UUID;

/**
 * Centralized ID generation utility.
 * Uses UUID v4 for all internal entity identifiers.
 */
public final class IdGenerator {

    private IdGenerator() {
        // Utility class — prevent instantiation
    }

    /**
     * Generate a new random UUID.
     */
    public static UUID newId() {
        return UUID.randomUUID();
    }

    /**
     * Generate a new UUID as a string (no hyphens).
     * Useful for reference IDs shown to users.
     */
    public static String newCompactId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
