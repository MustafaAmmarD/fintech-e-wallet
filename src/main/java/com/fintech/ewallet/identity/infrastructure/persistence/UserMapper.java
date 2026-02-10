package com.fintech.ewallet.identity.infrastructure.persistence;

import com.fintech.ewallet.identity.domain.AccountStatus;
import com.fintech.ewallet.identity.domain.KycStatus;
import com.fintech.ewallet.identity.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * MapStruct mapper for converting between {@link User} (domain) and
 * {@link UserJpaEntity} (JPA).
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    // ─── Domain → JPA Entity ──────────────────────────────────

    @Mapping(target = "kycStatus", source = "kycStatus", qualifiedByName = "kycStatusToString")
    @Mapping(target = "accountStatus", source = "accountStatus", qualifiedByName = "accountStatusToString")
    UserJpaEntity toJpaEntity(User user);

    // ─── JPA Entity → Domain ──────────────────────────────────

    @Mapping(target = "kycStatus", source = "kycStatus", qualifiedByName = "stringToKycStatus")
    @Mapping(target = "accountStatus", source = "accountStatus", qualifiedByName = "stringToAccountStatus")
    User toDomain(UserJpaEntity entity);

    // ─── Enum Converters ──────────────────────────────────────

    @Named("kycStatusToString")
    default String kycStatusToString(KycStatus status) {
        return status != null ? status.name() : null;
    }

    @Named("stringToKycStatus")
    default KycStatus stringToKycStatus(String status) {
        return status != null ? KycStatus.valueOf(status) : null;
    }

    @Named("accountStatusToString")
    default String accountStatusToString(AccountStatus status) {
        return status != null ? status.name() : null;
    }

    @Named("stringToAccountStatus")
    default AccountStatus stringToAccountStatus(String status) {
        return status != null ? AccountStatus.valueOf(status) : null;
    }
}
