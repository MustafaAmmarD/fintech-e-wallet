package com.fintech.ewallet.wallet.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port (interface) for P2PTransfer persistence.
 */
public interface TransferRepository {

    P2PTransfer save(P2PTransfer transfer);

    Optional<P2PTransfer> findById(UUID id);

    Optional<P2PTransfer> findByReferenceNo(String referenceNo);

    /**
     * Find all transfers where the user is either sender or recipient,
     * ordered by creation date descending.
     */
    List<P2PTransfer> findByUserIdOrderByCreatedAtDesc(UUID userId, int limit);
}
