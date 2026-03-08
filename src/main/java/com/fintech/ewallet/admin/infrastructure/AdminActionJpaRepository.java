package com.fintech.ewallet.admin.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AdminActionJpaRepository extends JpaRepository<AdminActionJpaEntity, UUID> {
}
