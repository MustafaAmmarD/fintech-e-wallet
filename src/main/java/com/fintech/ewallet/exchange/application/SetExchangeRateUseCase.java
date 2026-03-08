package com.fintech.ewallet.exchange.application;

import com.fintech.ewallet.exchange.application.dto.SetExchangeRateRequest;
import com.fintech.ewallet.exchange.application.dto.SetExchangeRateResponse;
import com.fintech.ewallet.exchange.domain.ExchangeRate;
import com.fintech.ewallet.exchange.domain.ExchangeRateRepository;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.wallet.domain.Currency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SetExchangeRateUseCase {

    private static final int RATE_SCALE = 8;

    private final ExchangeRateRepository exchangeRateRepository;
    private final UserRepository userRepository;

    @Transactional
    public SetExchangeRateResponse execute(UUID adminId, SetExchangeRateRequest request) {
        userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found: " + adminId));

        validateCurrencyPair(request.fromCurrency(), request.toCurrency());

        BigDecimal normalizedRate = request.rate().setScale(RATE_SCALE, RoundingMode.HALF_UP);
        if (normalizedRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Rate must be positive");
        }

        BigDecimal reverseRate = BigDecimal.ONE.divide(normalizedRate, RATE_SCALE, RoundingMode.HALF_UP);
        Instant effectiveAt = Instant.now();

        upsertRate(request.fromCurrency(), request.toCurrency(), normalizedRate, adminId, effectiveAt);
        upsertRate(request.toCurrency(), request.fromCurrency(), reverseRate, adminId, effectiveAt);

        return new SetExchangeRateResponse(
                request.fromCurrency(),
                request.toCurrency(),
                normalizedRate,
                reverseRate,
                effectiveAt);
    }

    private void validateCurrencyPair(Currency fromCurrency, Currency toCurrency) {
        if (fromCurrency == null || toCurrency == null) {
            throw new IllegalArgumentException("Both currencies are required");
        }
        if (fromCurrency == toCurrency) {
            throw new IllegalArgumentException("From and to currencies must be different");
        }
    }

    private void upsertRate(
            Currency fromCurrency,
            Currency toCurrency,
            BigDecimal rate,
            UUID adminId,
            Instant effectiveAt) {

        Optional<ExchangeRate> existing = exchangeRateRepository.findByCurrencyPair(fromCurrency, toCurrency);
        ExchangeRate rateToSave = existing
                .map(value -> value.withUpdatedRate(rate, adminId, effectiveAt))
                .orElseGet(() -> ExchangeRate.createNew(fromCurrency, toCurrency, rate, adminId, effectiveAt));

        exchangeRateRepository.save(rateToSave);
    }
}
