package com.fintech.ewallet.kyc.infrastructure.persistence;

import com.fintech.ewallet.kyc.domain.KycDocument;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for KycDocument domain and JPA entity conversion.
 */
@Mapper(componentModel = "spring")
public interface KycDocumentMapper {

    KycDocument toDomain(KycDocumentJpaEntity entity);

    KycDocumentJpaEntity toEntity(KycDocument domain);
}
