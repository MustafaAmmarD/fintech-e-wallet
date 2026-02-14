package com.fintech.ewallet.kyc.infrastructure.persistence;

import com.fintech.ewallet.identity.domain.KycStatus;
import com.fintech.ewallet.kyc.domain.KycDocument;
import com.fintech.ewallet.kyc.domain.KycDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository adapter implementing KycDocumentRepository using JPA.
 */
@Repository
@RequiredArgsConstructor
public class KycDocumentRepositoryAdapter implements KycDocumentRepository {

    private final KycDocumentJpaRepository jpaRepository;
    private final KycDocumentMapper mapper;

    @Override
    public KycDocument save(KycDocument document) {
        KycDocumentJpaEntity entity = mapper.toEntity(document);
        KycDocumentJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<KycDocument> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<KycDocument> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<KycDocument> findByStatus(KycStatus status) {
        return jpaRepository.findByStatus(status).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
