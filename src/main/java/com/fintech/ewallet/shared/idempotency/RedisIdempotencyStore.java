package com.fintech.ewallet.shared.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisIdempotencyStore implements IdempotencyStore {

    private static final String RESPONSE_PREFIX = "idempotency:response:";
    private static final String LOCK_PREFIX = "idempotency:lock:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<IdempotencyStoredResponse> findResponse(String key) {
        String value = redisTemplate.opsForValue().get(responseKey(key));
        if (value == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(value, IdempotencyStoredResponse.class));
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse cached idempotency response for key={}", key, e);
            redisTemplate.delete(responseKey(key));
            return Optional.empty();
        }
    }

    @Override
    public void saveResponse(String key, IdempotencyStoredResponse response, Duration ttl) {
        try {
            String serialized = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(responseKey(key), serialized, ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize idempotency response", e);
        }
    }

    @Override
    public boolean tryAcquireLock(String key, Duration ttl) {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey(key), "1", ttl);
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void releaseLock(String key) {
        redisTemplate.delete(lockKey(key));
    }

    private String responseKey(String key) {
        return RESPONSE_PREFIX + key;
    }

    private String lockKey(String key) {
        return LOCK_PREFIX + key;
    }
}
