package com.fintech.ewallet.wallet.infrastructure.persistence;

import com.fintech.ewallet.wallet.domain.LedgerEntry;
import com.fintech.ewallet.wallet.domain.LedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

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
        return jpaRepository.findByWalletIdOrderByCreatedAtDesc(walletId, PageRequest.of(0, limit))
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
