package com.fintech.ewallet.bill.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillerJpaRepository extends JpaRepository<BillerJpaEntity, UUID> {
    List<BillerJpaEntity> findByStatus(String status);

    List<BillerJpaEntity> findByStatusAndCategory(String status, String category);

    Optional<BillerJpaEntity> findByCode(String code);
}
