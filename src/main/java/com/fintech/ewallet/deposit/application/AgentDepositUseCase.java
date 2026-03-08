package com.fintech.ewallet.deposit.application;

import com.fintech.ewallet.deposit.application.dto.AgentDepositRequest;
import com.fintech.ewallet.deposit.application.dto.AgentDepositResponse;
import com.fintech.ewallet.deposit.domain.Deposit;
import com.fintech.ewallet.deposit.domain.DepositRepository;
import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.shared.privacy.NameMaskingService;
import com.fintech.ewallet.wallet.application.RecordLedgerEntryUseCase;
import com.fintech.ewallet.wallet.domain.ReferenceType;
import com.fintech.ewallet.wallet.domain.SystemWallets;
import com.fintech.ewallet.wallet.domain.Wallet;
import com.fintech.ewallet.wallet.domain.WalletRepository;
import com.fintech.ewallet.wallet.domain.WalletStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentDepositUseCase {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final DepositRepository depositRepository;
    private final RecordLedgerEntryUseCase recordLedgerEntryUseCase;
    private final NameMaskingService nameMaskingService;

    @Transactional
    public AgentDepositResponse execute(UUID agentId, AgentDepositRequest request) {
        userRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));

        User recipient = userRepository.findByAccountNumber(request.recipientAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No user found with account number: " + request.recipientAccountNumber()));

        Wallet recipientWallet = walletRepository.findByUserIdAndCurrency(recipient.getId(), request.currency())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Recipient doesn't have a " + request.currency() + " wallet"));

        if (recipientWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Recipient wallet is not active");
        }

        String description = normalizeDescription(request.description());

        Deposit deposit = new Deposit(
                recipient.getId(),
                agentId,
                recipientWallet.getId(),
                request.amount(),
                request.currency(),
                description);

        UUID liquidityWalletId = SystemWallets.getLiquidityWallet(request.currency());

        recordLedgerEntryUseCase.recordDoubleEntry(
                liquidityWalletId,
                recipientWallet.getId(),
                request.amount(),
                ReferenceType.DEPOSIT,
                deposit.getId(),
                description);

        Deposit savedDeposit = depositRepository.save(deposit);

        return new AgentDepositResponse(
                savedDeposit.getId(),
                savedDeposit.getReferenceNo(),
                nameMaskingService.getDisplayName(recipient, agentId),
                savedDeposit.getAmount(),
                savedDeposit.getCurrency(),
                savedDeposit.getStatus(),
                savedDeposit.getCreatedAt());
    }

    private String normalizeDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return "Agent cash deposit";
        }
        return description.trim();
    }
}
