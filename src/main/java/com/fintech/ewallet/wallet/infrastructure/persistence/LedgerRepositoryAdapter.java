package com.fintech.ewallet.wallet.infrastructure.persistence;

import com.fintech.ewallet.wallet.domain.EntryType;
import com.fintech.ewallet.wallet.domain.LedgerEntry;
import com.fintech.ewallet.wallet.domain.LedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LedgerRepositoryAdapter implements LedgerRepository {

    private final LedgerEntryJpaRepository jpaRepository;
    private final LedgerEntryMapper mapper;

    @Override
    public LedgerEntry save(LedgerEntry entry) {
        LedgerEntryJpaEntity entity = mapper.toEntity(entry);
        LedgerEntryJpaEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public List<LedgerEntry> saveAll(List<LedgerEntry> entries) {
        List<LedgerEntryJpaEntity> entities = entries.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());
        List<LedgerEntryJpaEntity> savedEntities = jpaRepository.saveAll(entities);
        return savedEntities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<LedgerEntry> findByWalletId(UUID walletId) {
        return jpaRepository.findByWalletId(walletId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<LedgerEntry> findByTransactionId(UUID transactionId) {
        return jpaRepository.findByTransactionId(transactionId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(UUID walletId, int limit) {
        return jpaRepository.findByWalletIdOrderByCreatedAtDesc(walletId, PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<LedgerEntry> findByWalletIdWithFilters(UUID walletId, EntryType type, Instant startDate, Instant endDate, int offset, int limit) {
        int page = offset / limit;
        return jpaRepository.findByWalletIdWithFilters(walletId, type, startDate, endDate, PageRequest.of(page, limit))
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByWalletIdWithFilters(UUID walletId, EntryType type, Instant startDate, Instant endDate) {
        return jpaRepository.countByWalletIdWithFilters(walletId, type, startDate, endDate);
    }

    @Override
    public BigDecimal sumDebitsByWalletIdBetween(UUID walletId, Instant fromInclusive, Instant toExclusive) {
        return jpaRepository.sumAmountByWalletIdAndEntryTypeBetween(
                walletId,
                EntryType.DEBIT,
                fromInclusive,
                toExclusive);
    }

    @Override
    public long countDebitsByWalletIdBetween(UUID walletId, Instant fromInclusive, Instant toExclusive) {
        return jpaRepository.countByWalletIdAndEntryTypeBetween(
                walletId,
                EntryType.DEBIT,
                fromInclusive,
                toExclusive);
    }

    @Override
    public boolean existsFinancialTransactionForUser(UUID userId) {
        return jpaRepository.existsFinancialTransactionForUser(userId);
    }
}
