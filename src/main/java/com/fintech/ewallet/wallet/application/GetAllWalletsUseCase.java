package com.fintech.ewallet.wallet.application;

import com.fintech.ewallet.wallet.application.dto.WalletSummary;
import com.fintech.ewallet.wallet.domain.Wallet;
import com.fintech.ewallet.wallet.domain.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use case for listing all wallets for a user.
 */
@Service
@RequiredArgsConstructor
public class GetAllWalletsUseCase {

    private final WalletRepository walletRepository;

    public List<WalletSummary> execute(UUID userId) {
        List<Wallet> wallets = walletRepository.findByUserId(userId);

        return wallets.stream()
                .map(wallet -> new WalletSummary(
                        wallet.getId(),
                        wallet.getCurrency(),
                        wallet.getBalance(),
                        wallet.getStatus()))
                .collect(Collectors.toList());
    }
}
