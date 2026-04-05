package com.fintech.ewallet.wallet.application;

import com.fintech.ewallet.wallet.application.dto.TransactionResponse;
import com.fintech.ewallet.wallet.domain.LedgerEntry;
import com.fintech.ewallet.wallet.domain.LedgerRepository;
import lombok.RequiredArgsConstructor;
import com.fintech.ewallet.shared.dto.PaginatedResponse;
import com.fintech.ewallet.wallet.domain.EntryType;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Use case for retrieving transaction history for a wallet.
 */
@Service
@RequiredArgsConstructor
public class GetTransactionHistoryUseCase {

    private final LedgerRepository ledgerRepository;
    public PaginatedResponse<TransactionResponse> execute(
            UUID walletId, EntryType type, Instant startDate, Instant endDate, int page, int size) {
        
        int offset = page * size;
        List<LedgerEntry> entries = ledgerRepository
                .findByWalletIdWithFilters(walletId, type, startDate, endDate, offset, size);

        long totalCount = ledgerRepository
                .countByWalletIdWithFilters(walletId, type, startDate, endDate);

        boolean isArabic = "ar".equals(LocaleContextHolder.getLocale().getLanguage());

        List<TransactionResponse> content = entries.stream()
                .map(entry -> new TransactionResponse(
                        entry.getId(),
                        entry.getTransactionId(),
                        entry.getEntryType(),
                        entry.getAmount(),
                        entry.getBalanceAfter(),
                        entry.getReferenceType(),
                        isArabic && entry.getDescriptionAr() != null ? entry.getDescriptionAr() : entry.getDescription(),
                        entry.getCreatedAt()))
                .collect(Collectors.toList());

        return PaginatedResponse.of(content, page, size, totalCount);
    }
}
