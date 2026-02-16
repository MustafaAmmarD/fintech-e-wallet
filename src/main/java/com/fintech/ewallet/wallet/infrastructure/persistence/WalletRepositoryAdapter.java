package com.fintech.ewallet.wallet.infrastructure.persistence;

import com.fintech.ewallet.wallet.domain.Currency;
import com.fintech.ewallet.wallet.domain.Wallet;
import com.fintech.ewallet.wallet.domain.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WalletRepositoryAdapter implements WalletRepository {

    private final WalletJpaRepository jpaRepository;
    private final WalletMapper mapper;

    @Override
    public Wallet save(Wallet wallet) {
        WalletJpaEntity entity = mapper.toEntity(wallet);
        WalletJpaEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Wallet> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Wallet> findByUserIdAndCurrency(UUID userId, Currency currency) {
        return jpaRepository.findByUserIdAndCurrency(userId, currency).map(mapper::toDomain);
    }

    @Override
    public List<Wallet> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByUserIdAndCurrency(UUID userId, Currency currency) {
        return jpaRepository.existsByUserIdAndCurrency(userId, currency);
    }
}
