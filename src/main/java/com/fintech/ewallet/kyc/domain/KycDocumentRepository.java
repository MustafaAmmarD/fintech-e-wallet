package com.fintech.ewallet.kyc.domain;

import com.fintech.ewallet.identity.domain.KycStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository port for KYC documents.
 */
public interface KycDocumentRepository {

    /**
     * Save a KYC document.
     */
    KycDocument save(KycDocument document);

    /**
     * Find document by ID.
     */
    Optional<KycDocument> findById(UUID id);

    /**
     * Find all documents for a user.
     */
    List<KycDocument> findByUserId(UUID userId);

    /**
     * Find all documents with a specific status (for admin dashboard).
     */
    List<KycDocument> findByStatus(KycStatus status);
}
