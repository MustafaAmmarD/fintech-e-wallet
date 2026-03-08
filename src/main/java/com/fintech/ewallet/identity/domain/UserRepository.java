package com.fintech.ewallet.identity.domain;

import java.util.List;
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

    Optional<User> findByAccountNumber(String accountNumber);

    Optional<User> findByReferralCode(String referralCode);

    List<User> findAll();

    List<User> findByKycStatus(KycStatus kycStatus);

    List<User> searchUsers(String query);
}
