package com.fintech.ewallet.identity.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Port (interface) for User persistence.
 * <p>
 * This is a domain-level abstraction. The infrastructure layer provides
 * the adapter implementation using Spring Data JPA.
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);
}
