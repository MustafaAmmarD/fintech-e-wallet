package com.fintech.ewallet.admin.application;

import com.fintech.ewallet.wallet.application.dto.TransactionResponse;
import com.fintech.ewallet.wallet.domain.LedgerEntry;
import com.fintech.ewallet.wallet.domain.LedgerRepository;
import com.fintech.ewallet.wallet.domain.Wallet;
import com.fintech.ewallet.wallet.domain.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetAdminTransactionsUseCase {

    private final WalletRepository walletRepository;
    private final LedgerRepository ledgerRepository;

    @Transactional(readOnly = true)
    public List<TransactionResponse> execute(UUID userId, int limit) {
        List<Wallet> wallets = walletRepository.findByUserId(userId);

        List<LedgerEntry> allEntries = new ArrayList<>();

        for (Wallet wallet : wallets) {
            allEntries.addAll(ledgerRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId(), limit));
        }

        // Sort globally by created at descending and apply limit
        return allEntries.stream()
                .sorted(Comparator.comparing(LedgerEntry::getCreatedAt).reversed())
                .limit(limit)
                .map(entry -> new TransactionResponse(
                        entry.getId(),
                        entry.getTransactionId(),
                        entry.getEntryType(),
                        entry.getAmount(),
                        entry.getBalanceAfter(),
                        entry.getReferenceType(),
                        entry.getDescription(),
                        entry.getCreatedAt()))
                .collect(Collectors.toList());
    }
}
