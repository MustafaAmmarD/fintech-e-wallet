package com.fintech.ewallet.fee.application;

import com.fintech.ewallet.fee.domain.FeeOperation;
import com.fintech.ewallet.fee.domain.FeeRule;
import com.fintech.ewallet.fee.domain.FeeRuleRepository;
import com.fintech.ewallet.fee.domain.FeeType;
import com.fintech.ewallet.wallet.domain.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CalculateFeeUseCase using Mockito.
 *
 * KEY CONCEPT: @Mock creates a FAKE FeeRuleRepository (no database connection).
 * We tell it exactly what to return using:
 * when(fakeRepo.findAll()).thenReturn(someList)
 * Then we verify the real use case calculates the correct fee.
 */
@ExtendWith(MockitoExtension.class)
class CalculateFeeUseCaseTest {

    @Mock
    private FeeRuleRepository feeRuleRepository;

    @InjectMocks
    private CalculateFeeUseCase calculateFeeUseCase;

    // ========================================================================
    // Helper: creates a flat fee rule (e.g. 50 YER regardless of amount)
    // ========================================================================
    private FeeRule flatFeeRule(FeeOperation op, Currency currency, BigDecimal flatAmount) {
        return new FeeRule(
                UUID.randomUUID(), op, currency, FeeType.FLAT,
                null, flatAmount,
                BigDecimal.ZERO, null,
                null, null,
                true, Instant.now(), Instant.now());
    }

    // ========================================================================
    // Helper: creates a percentage fee rule (e.g. 1.5% of the amount)
    // ========================================================================
    private FeeRule percentageFeeRule(FeeOperation op, Currency currency,
            BigDecimal rate, BigDecimal minFee, BigDecimal maxFee) {
        return new FeeRule(
                UUID.randomUUID(), op, currency, FeeType.PERCENTAGE,
                rate, null,
                BigDecimal.ZERO, null,
                minFee, maxFee,
                true, Instant.now(), Instant.now());
    }

    // ========================================================================
    // TEST 1: When a flat fee rule is in the DB, it overrides the 2% default
    // ========================================================================
    @Test
    @DisplayName("Should apply flat fee rule from database instead of default rate")
    void shouldApplyFlatFeeFromDatabaseRule() {
        // Arrange: fake repo returns a 50 YER flat fee rule
        FeeRule flatRule = flatFeeRule(FeeOperation.TRANSFER, Currency.YER, new BigDecimal("50"));
        when(feeRuleRepository.findActiveByOperationAndCurrency(FeeOperation.TRANSFER, Currency.YER))
                .thenReturn(List.of(flatRule));

        // Act: transfer 2500 YER — the flat rule should apply
        BigDecimal fee = calculateFeeUseCase.execute(
                FeeOperation.TRANSFER, Currency.YER, new BigDecimal("2500"));

        // Assert: flat rule of 50 YER wins (not 2% = 50, incidentally the same)
        assertEquals(0, new BigDecimal("50").compareTo(fee),
                "Expected flat fee of 50 YER, but got: " + fee);
    }

    // ========================================================================
    // TEST 2: When no DB rule found, fallback to default 2% transfer fee
    // ========================================================================
    @Test
    @DisplayName("Should use fallback 2% rate when no rule found in database")
    void shouldUseFallbackPercentageForTransfer() {
        // Arrange: fake repo returns EMPTY list (simulates no configured rules)
        when(feeRuleRepository.findActiveByOperationAndCurrency(any(), any()))
                .thenReturn(List.of());

        // Act: 1000 YER transfer with no DB rule
        BigDecimal fee = calculateFeeUseCase.execute(
                FeeOperation.TRANSFER, Currency.YER, new BigDecimal("1000"));

        // Assert: 2% of 1000 = 20 YER
        assertEquals(0, new BigDecimal("20").compareTo(fee),
                "Expected 2% fallback fee = 20 YER, but got: " + fee);
    }

    // ========================================================================
    // TEST 3: Fallback transfer fee should NOT go below minimum (1 YER)
    // ========================================================================
    @Test
    @DisplayName("Transfer fallback fee should enforce minimum of 1 YER")
    void transferFallbackFeeShouldRespectMinimum() {
        when(feeRuleRepository.findActiveByOperationAndCurrency(any(), any()))
                .thenReturn(List.of());

        // 2% of 10 YER = 0.20 YER — but minimum is 1 YER
        BigDecimal fee = calculateFeeUseCase.execute(
                FeeOperation.TRANSFER, Currency.YER, new BigDecimal("10"));

        assertEquals(0, new BigDecimal("1").compareTo(fee),
                "Expected minimum fee of 1 YER for tiny transfer, but got: " + fee);
    }

    // ========================================================================
    // TEST 4: Fallback transfer fee should NOT exceed maximum (500 YER)
    // ========================================================================
    @Test
    @DisplayName("Transfer fallback fee should enforce maximum of 500 YER")
    void transferFallbackFeeShouldRespectMaximum() {
        when(feeRuleRepository.findActiveByOperationAndCurrency(any(), any()))
                .thenReturn(List.of());

        // 2% of 1,000,000 = 20,000 YER — but maximum is 500 YER
        BigDecimal fee = calculateFeeUseCase.execute(
                FeeOperation.TRANSFER, Currency.YER, new BigDecimal("1000000"));

        assertEquals(0, new BigDecimal("500").compareTo(fee),
                "Expected maximum fee of 500 YER for large transfer, but got: " + fee);
    }

    // ========================================================================
    // TEST 5: Deposits should be FREE (0 fee) by default
    // ========================================================================
    @Test
    @DisplayName("Deposit should have zero fee when no rule is configured")
    void depositShouldHaveZeroFeeByDefault() {
        when(feeRuleRepository.findActiveByOperationAndCurrency(any(), any()))
                .thenReturn(List.of());

        BigDecimal fee = calculateFeeUseCase.execute(
                FeeOperation.DEPOSIT, Currency.YER, new BigDecimal("5000"));

        assertEquals(0, BigDecimal.ZERO.compareTo(fee),
                "Expected zero deposit fee, but got: " + fee);
    }

    // ========================================================================
    // TEST 6: Withdrawals should also be FREE (0 fee) by default
    // ========================================================================
    @Test
    @DisplayName("Withdrawal should have zero fee when no rule is configured")
    void withdrawalShouldHaveZeroFeeByDefault() {
        when(feeRuleRepository.findActiveByOperationAndCurrency(any(), any()))
                .thenReturn(List.of());

        BigDecimal fee = calculateFeeUseCase.execute(
                FeeOperation.WITHDRAWAL, Currency.YER, new BigDecimal("3000"));

        assertEquals(0, BigDecimal.ZERO.compareTo(fee),
                "Expected zero withdrawal fee, but got: " + fee);
    }

    // ========================================================================
    // TEST 7: Null operation type must throw IllegalArgumentException
    // ========================================================================
    @Test
    @DisplayName("Null operation type should throw IllegalArgumentException")
    void nullOperationShouldThrowException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> calculateFeeUseCase.execute(null, Currency.YER, new BigDecimal("1000")));
        assertEquals("Operation type is required", ex.getMessage());
    }

    // ========================================================================
    // TEST 8: Zero amount must be rejected
    // ========================================================================
    @Test
    @DisplayName("Zero amount should throw IllegalArgumentException")
    void zeroAmountShouldThrowException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> calculateFeeUseCase.execute(FeeOperation.TRANSFER, Currency.YER, BigDecimal.ZERO));
        assertEquals("Amount must be positive", ex.getMessage());
    }

    // ========================================================================
    // TEST 9: Percentage rule from DB with min/max caps is applied
    // ========================================================================
    @Test
    @DisplayName("Should apply percentage fee rule from database with min/max cap")
    void shouldApplyPercentageFeeFromDatabaseRule() {
        // Arrange: 1.5% rule with 10 YER min and 200 YER max
        FeeRule pctRule = percentageFeeRule(
                FeeOperation.TRANSFER, Currency.YER,
                new BigDecimal("0.015"),
                new BigDecimal("10"),
                new BigDecimal("200"));
        when(feeRuleRepository.findActiveByOperationAndCurrency(FeeOperation.TRANSFER, Currency.YER))
                .thenReturn(List.of(pctRule));

        // Act: 1.5% of 2000 = 30 YER (within 10-200 range)
        BigDecimal fee = calculateFeeUseCase.execute(
                FeeOperation.TRANSFER, Currency.YER, new BigDecimal("2000"));

        assertEquals(0, new BigDecimal("30").compareTo(fee),
                "Expected 1.5% of 2000 = 30 YER, but got: " + fee);
    }
}
