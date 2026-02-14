package com.fintech.ewallet.shared.security;

import java.time.Duration;

/**
 * Port (interface) for token blacklist operations.
 * <p>
 * Used to invalidate JWTs before their natural expiration (e.g., on logout).
 * The adapter implementation uses Redis for fast lookups with TTL.
 */
public interface TokenBlacklistService {

    /**
     * Add a token to the blacklist.
     *
     * @param tokenId Unique token ID (jti claim from JWT)
     * @param ttl     Time-to-live (should match token's remaining validity)
     */
    void blacklistToken(String tokenId, Duration ttl);

    /**
     * Check if a token is blacklisted.
     *
     * @param tokenId Unique token ID (jti claim from JWT)
     * @return true if the token is blacklisted
     */
    boolean isBlacklisted(String tokenId);
}
