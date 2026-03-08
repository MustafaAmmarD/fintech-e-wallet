package com.fintech.ewallet.shared.idempotency;

import java.time.Duration;
import java.util.Optional;

public interface IdempotencyStore {

    Optional<IdempotencyStoredResponse> findResponse(String key);

    void saveResponse(String key, IdempotencyStoredResponse response, Duration ttl);

    boolean tryAcquireLock(String key, Duration ttl);

    void releaseLock(String key);
}
