package com.fintech.ewallet.wallet.infrastructure.persistence;

import com.fintech.ewallet.wallet.domain.P2PTransfer;
import com.fintech.ewallet.wallet.domain.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransferRepositoryAdapter implements TransferRepository {

    private final TransferJpaRepository jpaRepository;
    private final TransferMapper mapper;

    @Override
    public P2PTransfer save(P2PTransfer transfer) {
        TransferJpaEntity entity = mapper.toEntity(transfer);
        TransferJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<P2PTransfer> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<P2PTransfer> findByReferenceNo(String referenceNo) {
        return jpaRepository.findByReferenceNo(referenceNo).map(mapper::toDomain);
    }

    @Override
    public List<P2PTransfer> findByUserIdOrderByCreatedAtDesc(UUID userId, int limit) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .limit(limit)
                .map(mapper::toDomain)
                .toList();
    }
}
