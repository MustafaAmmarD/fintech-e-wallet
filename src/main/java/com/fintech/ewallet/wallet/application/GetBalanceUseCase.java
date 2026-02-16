package com.fintech.ewallet.wallet.application;

import com.fintech.ewallet.wallet.application.dto.BalanceResponse;
import com.fintech.ewallet.wallet.domain.Wallet;
import com.fintech.ewallet.wallet.domain.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Use case for retrieving wallet balance.
 * Returns cached balance from wallets table for performance.
 */
@Service
@RequiredArgsConstructor
public class GetBalanceUseCase {

    private final WalletRepository walletRepository;

    public BalanceResponse execute(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        return new BalanceResponse(
                wallet.getId(),
                wallet.getCurrency(),
                wallet.getBalance(), // Cached balance
                wallet.getStatus());
    }
}
