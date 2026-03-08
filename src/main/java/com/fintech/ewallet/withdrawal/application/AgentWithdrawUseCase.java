package com.fintech.ewallet.withdrawal.application;

import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.shared.privacy.NameMaskingService;
import com.fintech.ewallet.wallet.application.RecordLedgerEntryUseCase;
import com.fintech.ewallet.wallet.domain.ReferenceType;
import com.fintech.ewallet.wallet.domain.SystemWallets;
import com.fintech.ewallet.wallet.domain.Wallet;
import com.fintech.ewallet.wallet.domain.WalletRepository;
import com.fintech.ewallet.wallet.domain.WalletStatus;
import com.fintech.ewallet.withdrawal.application.dto.AgentWithdrawRequest;
import com.fintech.ewallet.withdrawal.application.dto.AgentWithdrawResponse;
import com.fintech.ewallet.withdrawal.domain.Withdrawal;
import com.fintech.ewallet.withdrawal.domain.WithdrawalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentWithdrawUseCase {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final RecordLedgerEntryUseCase recordLedgerEntryUseCase;
    private final NameMaskingService nameMaskingService;

    @Transactional
    public AgentWithdrawResponse execute(UUID agentId, AgentWithdrawRequest request) {
        userRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));

        User user = userRepository.findByAccountNumber(request.userAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No user found with account number: " + request.userAccountNumber()));

        Wallet userWallet = walletRepository.findByUserIdAndCurrency(user.getId(), request.currency())
                .orElseThrow(() -> new IllegalArgumentException(
                        "User doesn't have a " + request.currency() + " wallet"));

        if (userWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("User wallet is not active");
        }

        String description = normalizeDescription(request.description());

        Withdrawal withdrawal = new Withdrawal(
                user.getId(),
                agentId,
                userWallet.getId(),
                request.amount(),
                request.currency(),
                description);

        UUID liquidityWalletId = SystemWallets.getLiquidityWallet(request.currency());

        recordLedgerEntryUseCase.recordDoubleEntry(
                userWallet.getId(),
                liquidityWalletId,
                request.amount(),
                ReferenceType.WITHDRAWAL,
                withdrawal.getId(),
                description);

        Withdrawal savedWithdrawal = withdrawalRepository.save(withdrawal);

        return new AgentWithdrawResponse(
                savedWithdrawal.getId(),
                savedWithdrawal.getReferenceNo(),
                nameMaskingService.getDisplayName(user, agentId),
                savedWithdrawal.getAmount(),
                savedWithdrawal.getCurrency(),
                savedWithdrawal.getStatus(),
                savedWithdrawal.getCreatedAt());
    }

    private String normalizeDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return "Agent cash withdrawal";
        }
        return description.trim();
    }
}
