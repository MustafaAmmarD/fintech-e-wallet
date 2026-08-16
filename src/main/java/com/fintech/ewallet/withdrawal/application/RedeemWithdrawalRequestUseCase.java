package com.fintech.ewallet.withdrawal.application;

import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.shared.privacy.NameMaskingService;
import com.fintech.ewallet.wallet.application.RecordLedgerEntryUseCase;
import com.fintech.ewallet.wallet.domain.ReferenceType;
import com.fintech.ewallet.wallet.domain.SystemWallets;
import com.fintech.ewallet.wallet.domain.Wallet;
import com.fintech.ewallet.wallet.domain.WalletRepository;
import com.fintech.ewallet.withdrawal.application.dto.AgentWithdrawResponse;
import com.fintech.ewallet.withdrawal.application.dto.RedeemWithdrawalDto;
import com.fintech.ewallet.withdrawal.domain.Withdrawal;
import com.fintech.ewallet.withdrawal.domain.WithdrawalRepository;
import com.fintech.ewallet.withdrawal.domain.WithdrawalRequest;
import com.fintech.ewallet.withdrawal.domain.WithdrawalRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedeemWithdrawalRequestUseCase {

    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final RecordLedgerEntryUseCase recordLedgerEntryUseCase;
    private final NameMaskingService nameMaskingService;

    @Transactional
    public AgentWithdrawResponse execute(UUID agentId, RedeemWithdrawalDto request) {
        userRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found"));

        WithdrawalRequest withdrawalRequest = withdrawalRequestRepository.findByWithdrawalCode(request.withdrawalCode())
                .orElseThrow(() -> new IllegalArgumentException("Invalid withdrawal code"));

        User user = userRepository.findById(withdrawalRequest.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Wallet userWallet = walletRepository.findByIdForUpdate(withdrawalRequest.getWalletId())
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        if (userWallet.getBalance().compareTo(withdrawalRequest.getAmount()) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }

        withdrawalRequest.redeem();
        withdrawalRequestRepository.save(withdrawalRequest);

        String description = "Cash withdrawal via code " + request.withdrawalCode();
        String descriptionAr = "سحب نقدي عبر الرمز " + request.withdrawalCode();

        Withdrawal withdrawal = new Withdrawal(
                user.getId(),
                agentId,
                userWallet.getId(),
                withdrawalRequest.getAmount(),
                withdrawalRequest.getCurrency(),
                description
        );

        UUID liquidityWalletId = SystemWallets.getLiquidityWallet(withdrawalRequest.getCurrency());

        recordLedgerEntryUseCase.recordDoubleEntry(
                userWallet.getId(),
                liquidityWalletId,
                withdrawalRequest.getAmount(),
                ReferenceType.WITHDRAWAL,
                withdrawal.getId(), // the actual withdrawal ID
                description,
                descriptionAr
        );

        Withdrawal savedWithdrawal = withdrawalRepository.save(withdrawal);

        return new AgentWithdrawResponse(
                savedWithdrawal.getId(),
                savedWithdrawal.getReferenceNo(),
                nameMaskingService.getDisplayName(user, agentId),
                savedWithdrawal.getAmount(),
                savedWithdrawal.getCurrency(),
                savedWithdrawal.getStatus(),
                savedWithdrawal.getCreatedAt()
        );
    }
}
