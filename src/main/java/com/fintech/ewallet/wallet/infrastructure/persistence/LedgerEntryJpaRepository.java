package com.fintech.ewallet.wallet.infrastructure.persistence;

import com.fintech.ewallet.wallet.domain.EntryType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryJpaEntity, UUID> {
    List<LedgerEntryJpaEntity> findByWalletId(UUID walletId);

    List<LedgerEntryJpaEntity> findByTransactionId(UUID transactionId);

    List<LedgerEntryJpaEntity> findByWalletIdOrderByCreatedAtDesc(UUID walletId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM LedgerEntryJpaEntity e " +
            "WHERE e.walletId = :walletId " +
            "AND e.entryType = :entryType " +
            "AND e.createdAt >= :fromInclusive " +
            "AND e.createdAt < :toExclusive")
    BigDecimal sumAmountByWalletIdAndEntryTypeBetween(
            @Param("walletId") UUID walletId,
            @Param("entryType") EntryType entryType,
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive);

    @Query("SELECT COUNT(e) FROM LedgerEntryJpaEntity e " +
            "WHERE e.walletId = :walletId " +
            "AND e.entryType = :entryType " +
            "AND e.createdAt >= :fromInclusive " +
            "AND e.createdAt < :toExclusive")
    long countByWalletIdAndEntryTypeBetween(
            @Param("walletId") UUID walletId,
            @Param("entryType") EntryType entryType,
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive);

    @Query(value = """
            SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END
            FROM ledger_entries le
            JOIN wallets w ON w.id = le.wallet_id
            WHERE w.user_id = :userId
              AND le.reference_type IN ('TRANSFER', 'DEPOSIT', 'WITHDRAWAL', 'EXCHANGE')
            """, nativeQuery = true)
    boolean existsFinancialTransactionForUser(@Param("userId") UUID userId);
}
