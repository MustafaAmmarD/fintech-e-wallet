package com.fintech.ewallet.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link UserJpaEntity}.
 */
@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    Optional<UserJpaEntity> findByAccountNumber(String accountNumber);

    Optional<UserJpaEntity> findByReferralCode(String referralCode);

    List<UserJpaEntity> findByKycStatus(String kycStatus);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM UserJpaEntity u WHERE " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "u.phoneNumber LIKE CONCAT('%', :keyword, '%') OR " +
            "u.accountNumber LIKE CONCAT('%', :keyword, '%')")
    List<UserJpaEntity> searchByKeyword(@org.springframework.data.repository.query.Param("keyword") String keyword);
}
