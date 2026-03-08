package com.fintech.ewallet.device.infrastructure.persistence;

import com.fintech.ewallet.device.domain.TrustedDevice;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for TrustedDevice <-> JPA entity.
 */
@Mapper(componentModel = "spring")
public interface TrustedDeviceMapper {

    TrustedDevice toDomain(TrustedDeviceJpaEntity entity);

    TrustedDeviceJpaEntity toEntity(TrustedDevice domain);
}
