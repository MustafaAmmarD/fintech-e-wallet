package com.fintech.ewallet.exchange.application;

import com.fintech.ewallet.exchange.application.dto.ExchangeQuoteResponse;
import com.fintech.ewallet.exchange.domain.ExchangeQuote;
import com.fintech.ewallet.exchange.domain.ExchangeQuoteRepository;
import com.fintech.ewallet.exchange.domain.ExchangeRate;
import com.fintech.ewallet.exchange.domain.ExchangeRateRepository;
import com.fintech.ewallet.fee.application.CalculateFeeUseCase;
import com.fintech.ewallet.fee.domain.FeeOperation;
import com.fintech.ewallet.shared.exception.ExchangeRateNotFoundException;
import com.fintech.ewallet.wallet.domain.Currency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetExchangeQuoteUseCase {

    private static final int MONEY_SCALE = 4;
    private static final long QUOTE_TTL_SECONDS = 30L;

    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeQuoteRepository exchangeQuoteRepository;
    private final CalculateFeeUseCase calculateFeeUseCase;

    @Transactional
    public ExchangeQuoteResponse execute(
            UUID userId,
            Currency fromCurrency,
            Currency toCurrency,
            BigDecimal amount) {
        validateRequest(fromCurrency, toCurrency, amount);

        BigDecimal normalizedAmount = amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        ExchangeRate exchangeRate = exchangeRateRepository.findByCurrencyPair(fromCurrency, toCurrency)
                .orElseThrow(() -> new ExchangeRateNotFoundException(fromCurrency, toCurrency));

        BigDecimal toAmount = normalizedAmount.multiply(exchangeRate.getRate()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal feeAmount = calculateFeeUseCase.execute(FeeOperation.EXCHANGE, fromCurrency, normalizedAmount);
        BigDecimal totalDeducted = normalizedAmount.add(feeAmount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        Instant expiresAt = Instant.now().plusSeconds(QUOTE_TTL_SECONDS);

        ExchangeQuote quote = new ExchangeQuote(
                userId,
                fromCurrency,
                toCurrency,
                normalizedAmount,
                toAmount,
                exchangeRate.getRate(),
                feeAmount,
                totalDeducted,
                expiresAt);

        ExchangeQuote savedQuote = exchangeQuoteRepository.save(quote);

        return new ExchangeQuoteResponse(
                savedQuote.getId(),
                savedQuote.getFromCurrency(),
                savedQuote.getToCurrency(),
                savedQuote.getFromAmount(),
                savedQuote.getToAmount(),
                savedQuote.getRate(),
                savedQuote.getFeeAmount(),
                savedQuote.getTotalDeducted(),
                savedQuote.getExpiresAt());
    }

    private void validateRequest(Currency fromCurrency, Currency toCurrency, BigDecimal amount) {
        if (fromCurrency == null || toCurrency == null) {
            throw new IllegalArgumentException("Both currencies are required");
        }
        if (fromCurrency == toCurrency) {
            throw new IllegalArgumentException("From and to currencies must be different");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
}
