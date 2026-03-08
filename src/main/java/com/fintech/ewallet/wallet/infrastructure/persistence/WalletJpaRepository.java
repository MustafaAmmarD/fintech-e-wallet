package com.fintech.ewallet.wallet.infrastructure.persistence;

import com.fintech.ewallet.wallet.domain.Currency;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletJpaRepository extends JpaRepository<WalletJpaEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WalletJpaEntity w WHERE w.id = :id")
    Optional<WalletJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    List<WalletJpaEntity> findByUserId(UUID userId);

    Optional<WalletJpaEntity> findByUserIdAndCurrency(UUID userId, Currency currency);

    boolean existsByUserIdAndCurrency(UUID userId, Currency currency);
}
