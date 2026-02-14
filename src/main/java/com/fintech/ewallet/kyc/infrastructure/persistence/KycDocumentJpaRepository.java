package com.fintech.ewallet.kyc.infrastructure.persistence;

import com.fintech.ewallet.identity.domain.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for KYC documents.
 */
public interface KycDocumentJpaRepository extends JpaRepository<KycDocumentJpaEntity, UUID> {

    List<KycDocumentJpaEntity> findByUserId(UUID userId);

    List<KycDocumentJpaEntity> findByStatus(KycStatus status);
}
