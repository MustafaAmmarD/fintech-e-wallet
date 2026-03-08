package com.fintech.ewallet.bill.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BillPaymentJpaRepository extends JpaRepository<BillPaymentJpaEntity, UUID> {
    List<BillPaymentJpaEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
