package com.fintech.ewallet.admin.infrastructure;

import com.fintech.ewallet.admin.domain.AdminAction;
import com.fintech.ewallet.admin.domain.AdminActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AdminActionRepositoryAdapter implements AdminActionRepository {

    private final AdminActionJpaRepository jpaRepository;
    private final AdminActionMapper mapper;

    @Override
    public AdminAction save(AdminAction adminAction) {
        AdminActionJpaEntity entity = mapper.toEntity(adminAction);
        AdminActionJpaEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }
}
