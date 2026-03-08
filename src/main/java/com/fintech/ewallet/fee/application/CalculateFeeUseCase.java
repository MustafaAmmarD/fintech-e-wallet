package com.fintech.ewallet.fee.application;

import com.fintech.ewallet.fee.domain.FeeOperation;
import com.fintech.ewallet.fee.domain.FeeRule;
import com.fintech.ewallet.fee.domain.FeeRuleRepository;
import com.fintech.ewallet.wallet.domain.Currency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalculateFeeUseCase {

    private static final int MONEY_SCALE = 4;
    private static final BigDecimal ZERO = new BigDecimal("0.0000");
    private static final BigDecimal TRANSFER_DEFAULT_RATE = new BigDecimal("0.02");
    private static final BigDecimal TRANSFER_DEFAULT_MIN = new BigDecimal("1.00");
    private static final BigDecimal TRANSFER_DEFAULT_MAX = new BigDecimal("500.00");
    private static final BigDecimal EXCHANGE_DEFAULT_RATE = new BigDecimal("0.01");

    private final FeeRuleRepository feeRuleRepository;

    public BigDecimal execute(FeeOperation operationType, Currency currency, BigDecimal amount) {
        if (operationType == null) {
            throw new IllegalArgumentException("Operation type is required");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        List<FeeRule> rules = feeRuleRepository.findActiveByOperationAndCurrency(operationType, currency);
        for (FeeRule feeRule : rules) {
            if (feeRule.matchesAmount(amount)) {
                return feeRule.calculate(amount);
            }
        }

        return fallbackFee(operationType, amount);
    }

    private BigDecimal fallbackFee(FeeOperation operationType, BigDecimal amount) {
        return switch (operationType) {
            case TRANSFER -> clamp(
                    amount.multiply(TRANSFER_DEFAULT_RATE).setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                    TRANSFER_DEFAULT_MIN,
                    TRANSFER_DEFAULT_MAX);
            case EXCHANGE -> amount.multiply(EXCHANGE_DEFAULT_RATE).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            case DEPOSIT, WITHDRAWAL -> ZERO;
        };
    }

    private BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
        BigDecimal clamped = value;
        if (clamped.compareTo(min) < 0) {
            clamped = min;
        }
        if (clamped.compareTo(max) > 0) {
            clamped = max;
        }
        return clamped.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
