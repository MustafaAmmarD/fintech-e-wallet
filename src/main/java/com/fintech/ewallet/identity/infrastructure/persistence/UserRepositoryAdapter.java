package com.fintech.ewallet.identity.infrastructure.persistence;

import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter that implements the domain {@link UserRepository} port
 * using Spring Data JPA and MapStruct.
 */
@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final UserMapper userMapper;

    @Override
    public User save(User user) {
        UserJpaEntity entity = userMapper.toJpaEntity(user);
        UserJpaEntity saved = jpaRepository.save(entity);
        return userMapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findByPhoneNumber(String phoneNumber) {
        return jpaRepository.findByPhoneNumber(phoneNumber)
                .map(userMapper::toDomain);
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return jpaRepository.existsByPhoneNumber(phoneNumber);
    }

    @Override
    public Optional<User> findByAccountNumber(String accountNumber) {
        return jpaRepository.findByAccountNumber(accountNumber)
                .map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findByReferralCode(String referralCode) {
        return jpaRepository.findByReferralCode(referralCode)
                .map(userMapper::toDomain);
    }

    @Override
    public List<User> findAll() {
        return jpaRepository.findAll().stream()
                .map(userMapper::toDomain)
                .toList();
    }

    @Override
    public List<User> searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return findAll();
        }
        return jpaRepository.searchByKeyword(query.trim()).stream()
                .map(userMapper::toDomain)
                .toList();
    }

    @Override
    public List<User> findByKycStatus(com.fintech.ewallet.identity.domain.KycStatus kycStatus) {
        return jpaRepository.findByKycStatus(kycStatus.name()).stream()
                .map(userMapper::toDomain)
                .toList();
    }
}
