package com.fintech.ewallet.identity.infrastructure.security;

import com.fintech.ewallet.shared.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-based token blacklist implementation.
 * <p>
 * Stores blacklisted token IDs in Redis with TTL (time-to-live).
 * Redis automatically removes expired entries, so no cleanup job is needed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisTokenBlacklistService implements TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "blacklist:token:";

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void blacklistToken(String tokenId, Duration ttl) {
        String key = BLACKLIST_PREFIX + tokenId;
        redisTemplate.opsForValue().set(key, "blacklisted", ttl);
        log.debug("Token blacklisted: {} (TTL: {})", tokenId, ttl);
    }

    @Override
    public boolean isBlacklisted(String tokenId) {
        String key = BLACKLIST_PREFIX + tokenId;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }
}
