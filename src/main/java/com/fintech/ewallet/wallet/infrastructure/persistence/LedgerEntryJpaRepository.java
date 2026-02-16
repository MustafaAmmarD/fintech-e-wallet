package com.fintech.ewallet.wallet.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryJpaEntity, UUID> {
    List<LedgerEntryJpaEntity> findByWalletId(UUID walletId);

    List<LedgerEntryJpaEntity> findByTransactionId(UUID transactionId);

    List<LedgerEntryJpaEntity> findByWalletIdOrderByCreatedAtDesc(UUID walletId, Pageable pageable);
}
