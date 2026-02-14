package com.fintech.ewallet.device.infrastructure.persistence;

import com.fintech.ewallet.device.domain.TrustedDevice;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for TrustedDevice <-> JPA entity.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TrustedDeviceMapper {

    TrustedDevice toDomain(TrustedDeviceJpaEntity entity);

    TrustedDeviceJpaEntity toEntity(TrustedDevice domain);
}
