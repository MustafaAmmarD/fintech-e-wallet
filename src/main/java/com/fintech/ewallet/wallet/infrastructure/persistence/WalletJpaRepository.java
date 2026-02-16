package com.fintech.ewallet.wallet.infrastructure.persistence;

import com.fintech.ewallet.wallet.domain.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletJpaRepository extends JpaRepository<WalletJpaEntity, UUID> {
    List<WalletJpaEntity> findByUserId(UUID userId);

    Optional<WalletJpaEntity> findByUserIdAndCurrency(UUID userId, Currency currency);

    boolean existsByUserIdAndCurrency(UUID userId, Currency currency);
}
