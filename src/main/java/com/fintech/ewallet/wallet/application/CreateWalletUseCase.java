package com.fintech.ewallet.wallet.application;

import com.fintech.ewallet.wallet.domain.Currency;
import com.fintech.ewallet.wallet.domain.Wallet;
import com.fintech.ewallet.wallet.domain.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateWalletUseCase {

    private final WalletRepository walletRepository;

    @Transactional
    public void createWalletsForUser(UUID userId) {
        // Create YER Wallet if not exists
        if (!walletRepository.existsByUserIdAndCurrency(userId, Currency.YER)) {
            walletRepository.save(new Wallet(userId, Currency.YER));
        }

        // Create SAR Wallet if not exists
        if (!walletRepository.existsByUserIdAndCurrency(userId, Currency.SAR)) {
            walletRepository.save(new Wallet(userId, Currency.SAR));
        }

        // Create USD Wallet if not exists
        if (!walletRepository.existsByUserIdAndCurrency(userId, Currency.USD)) {
            walletRepository.save(new Wallet(userId, Currency.USD));
        }
    }
}
