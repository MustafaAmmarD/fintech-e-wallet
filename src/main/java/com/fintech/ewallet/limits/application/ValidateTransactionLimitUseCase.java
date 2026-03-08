package com.fintech.ewallet.limits.application;

import com.fintech.ewallet.identity.domain.KycStatus;
import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.limits.domain.LimitOperationType;
import com.fintech.ewallet.limits.domain.LimitType;
import com.fintech.ewallet.limits.domain.TransactionLimit;
import com.fintech.ewallet.limits.domain.TransactionLimitRepository;
import com.fintech.ewallet.limits.domain.UserTier;
import com.fintech.ewallet.wallet.domain.Currency;
import com.fintech.ewallet.wallet.domain.LedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ValidateTransactionLimitUseCase {

    private final TransactionLimitRepository transactionLimitRepository;
    private final LedgerRepository ledgerRepository;
    private final UserRepository userRepository;

    public void validateDebit(
            UUID walletId,
            UUID userId,
            Currency currency,
            LimitOperationType operationType,
            BigDecimal requestedDebit) {

        if (userId == null || operationType == null) {
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Wallet owner not found: " + userId));
        UserTier userTier = resolveUserTier(user);

        List<TransactionLimit> limits = transactionLimitRepository.findActiveByCriteria(userTier, operationType, currency);
        if (limits.isEmpty()) {
            return;
        }

        for (TransactionLimit limit : limits) {
            applyLimit(walletId, requestedDebit, limit);
        }
    }

    private UserTier resolveUserTier(User user) {
        return user.getKycStatus() == KycStatus.VERIFIED
                ? UserTier.VERIFIED
                : UserTier.BASIC;
    }

    private void applyLimit(UUID walletId, BigDecimal requestedDebit, TransactionLimit limit) {
        if (limit.getLimitType() == LimitType.PER_TRANSACTION && limit.getMaxAmount() != null) {
            if (requestedDebit.compareTo(limit.getMaxAmount()) > 0) {
                throw new IllegalArgumentException(
                        "Amount exceeds max per transaction limit of " + normalizeNumber(limit.getMaxAmount()));
            }
            return;
        }

        if (limit.getLimitType() == LimitType.DAILY && limit.getMaxAmount() != null) {
            Instant fromInclusive = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant toExclusive = fromInclusive.plusSeconds(24 * 60 * 60);
            BigDecimal currentUsage = ledgerRepository.sumDebitsByWalletIdBetween(walletId, fromInclusive, toExclusive);
            ensureProjectedUsageWithinLimit("Daily", currentUsage, requestedDebit, limit.getMaxAmount());
            return;
        }

        if (limit.getLimitType() == LimitType.MONTHLY && limit.getMaxAmount() != null) {
            LocalDate firstDayOfMonth = LocalDate.now(ZoneOffset.UTC).with(TemporalAdjusters.firstDayOfMonth());
            Instant fromInclusive = firstDayOfMonth.atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant toExclusive = fromInclusive.plusSeconds(firstDayOfMonth.lengthOfMonth() * 24L * 60L * 60L);
            BigDecimal currentUsage = ledgerRepository.sumDebitsByWalletIdBetween(walletId, fromInclusive, toExclusive);
            ensureProjectedUsageWithinLimit("Monthly", currentUsage, requestedDebit, limit.getMaxAmount());
            return;
        }

        if (limit.getLimitType() == LimitType.VELOCITY
                && limit.getWindowHours() != null
                && limit.getMaxCount() != null) {
            Instant toExclusive = Instant.now();
            Instant fromInclusive = toExclusive.minusSeconds(limit.getWindowHours() * 3600L);
            long recentCount = ledgerRepository.countDebitsByWalletIdBetween(walletId, fromInclusive, toExclusive);
            if (recentCount >= limit.getMaxCount()) {
                throw new IllegalStateException(
                        "Velocity limit exceeded. Max " + limit.getMaxCount() + " transactions per "
                                + limit.getWindowHours() + " hour(s).");
            }
        }
    }

    private void ensureProjectedUsageWithinLimit(
            String scope,
            BigDecimal currentUsage,
            BigDecimal requestedDebit,
            BigDecimal configuredLimit) {
        BigDecimal projectedUsage = currentUsage.add(requestedDebit);
        if (projectedUsage.compareTo(configuredLimit) > 0) {
            throw new IllegalStateException(
                    scope + " transaction limit exceeded. Limit: " + normalizeNumber(configuredLimit)
                            + ", used: " + normalizeNumber(currentUsage)
                            + ", attempted: " + normalizeNumber(requestedDebit));
        }
    }

    private String normalizeNumber(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
