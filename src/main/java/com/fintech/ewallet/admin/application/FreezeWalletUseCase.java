package com.fintech.ewallet.admin.application;

import com.fintech.ewallet.admin.application.dto.FreezeWalletRequest;
import com.fintech.ewallet.admin.application.dto.WalletActionResponse;
import com.fintech.ewallet.admin.domain.AdminAction;
import com.fintech.ewallet.admin.domain.AdminActionRepository;
import com.fintech.ewallet.wallet.domain.Wallet;
import com.fintech.ewallet.wallet.domain.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FreezeWalletUseCase {

    private final WalletRepository walletRepository;
    private final AdminActionRepository adminActionRepository;

    @Transactional
    public WalletActionResponse execute(UUID adminId, UUID walletId, FreezeWalletRequest request) {
        Wallet wallet = walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        wallet.freeze();
        walletRepository.save(wallet);

        AdminAction action = new AdminAction(
                adminId,
                "FREEZE_WALLET",
                "WALLET",
                wallet.getId(),
                request.getReason());
        adminActionRepository.save(action);

        return new WalletActionResponse(wallet.getId(), wallet.getStatus().name(), "Wallet successfully frozen");
    }
}
