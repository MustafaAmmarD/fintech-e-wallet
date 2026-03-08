package com.fintech.ewallet.wallet.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransferJpaRepository extends JpaRepository<TransferJpaEntity, UUID> {

    Optional<TransferJpaEntity> findByReferenceNo(String referenceNo);

    /**
     * Find all transfers where the user is sender OR recipient, ordered by creation
     * date descending.
     */
    @Query("SELECT t FROM TransferJpaEntity t WHERE t.senderUserId = :userId OR t.recipientUserId = :userId ORDER BY t.createdAt DESC")
    List<TransferJpaEntity> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);
}
