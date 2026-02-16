package com.fintech.ewallet.wallet.application;

import com.fintech.ewallet.wallet.application.dto.TransactionResponse;
import com.fintech.ewallet.wallet.domain.LedgerEntry;
import com.fintech.ewallet.wallet.domain.LedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use case for retrieving transaction history for a wallet.
 */
@Service
@RequiredArgsConstructor
public class GetTransactionHistoryUseCase {

    private final LedgerRepository ledgerRepository;

    public List<TransactionResponse> execute(UUID walletId, int limit) {
        List<LedgerEntry> entries = ledgerRepository
                .findByWalletIdOrderByCreatedAtDesc(walletId, limit);

        return entries.stream()
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
