package com.fintech.ewallet.wallet.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository {
    Wallet save(Wallet wallet);

    Optional<Wallet> findById(UUID id);

    Optional<Wallet> findByUserIdAndCurrency(UUID userId, Currency currency);

    List<Wallet> findByUserId(UUID userId);

    boolean existsByUserIdAndCurrency(UUID userId, Currency currency);
}
