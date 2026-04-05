package com.fintech.ewallet.exchange.application;

import com.fintech.ewallet.exchange.application.dto.ExecuteExchangeRequest;
import com.fintech.ewallet.exchange.application.dto.ExecuteExchangeResponse;
import com.fintech.ewallet.exchange.domain.Exchange;
import com.fintech.ewallet.exchange.domain.ExchangeQuote;
import com.fintech.ewallet.exchange.domain.ExchangeQuoteRepository;
import com.fintech.ewallet.exchange.domain.ExchangeRate;
import com.fintech.ewallet.exchange.domain.ExchangeRateRepository;
import com.fintech.ewallet.exchange.domain.ExchangeRepository;
import com.fintech.ewallet.exchange.domain.QuoteStatus;
import com.fintech.ewallet.shared.exception.ExchangeQuoteExpiredException;
import com.fintech.ewallet.shared.exception.ExchangeQuoteNotFoundException;
import com.fintech.ewallet.shared.exception.ExchangeQuoteNotPendingException;
import com.fintech.ewallet.shared.exception.ExchangeRateNotFoundException;
import com.fintech.ewallet.shared.exception.ExchangeSlippageExceededException;
import com.fintech.ewallet.wallet.application.RecordLedgerEntryUseCase;
import com.fintech.ewallet.wallet.domain.SystemWallets;
import com.fintech.ewallet.wallet.domain.Wallet;
import com.fintech.ewallet.wallet.domain.WalletRepository;
import com.fintech.ewallet.wallet.domain.WalletStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExecuteExchangeUseCase {

    private static final BigDecimal MAX_SLIPPAGE_BPS = new BigDecimal("50.00");
    private static final BigDecimal BPS_MULTIPLIER = new BigDecimal("10000");
    private static final int RATE_SCALE = 8;
    private static final int BPS_SCALE = 2;
    private static final BigDecimal ZERO_BPS = new BigDecimal("0.00");

    private final ExchangeQuoteRepository exchangeQuoteRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRepository exchangeRepository;
    private final WalletRepository walletRepository;
    private final RecordLedgerEntryUseCase recordLedgerEntryUseCase;
    private final RecordFailedExchangeUseCase recordFailedExchangeUseCase;

    @Transactional
    public ExecuteExchangeResponse execute(UUID userId, ExecuteExchangeRequest request) {
        ExchangeQuote quote = exchangeQuoteRepository.findById(request.quoteId())
                .orElseThrow(() -> new ExchangeQuoteNotFoundException(request.quoteId()));

        if (!quote.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Quote does not belong to authenticated user");
        }

        if (quote.getStatus() != QuoteStatus.PENDING) {
            recordFailedExchangeUseCase.execute(quote, quote.getRate(), ZERO_BPS);
            throw new ExchangeQuoteNotPendingException(quote.getStatus());
        }

        if (quote.getExpiresAt().isBefore(Instant.now())) {
            recordFailedExchangeUseCase.execute(quote, quote.getRate(), ZERO_BPS);
            throw new ExchangeQuoteExpiredException();
        }

        ExchangeRate currentRate = exchangeRateRepository
                .findByCurrencyPair(quote.getFromCurrency(), quote.getToCurrency())
                .orElseThrow(() -> {
                    recordFailedExchangeUseCase.execute(quote, quote.getRate(), ZERO_BPS);
                    return new ExchangeRateNotFoundException(quote.getFromCurrency(), quote.getToCurrency());
                });

        BigDecimal slippageBps = calculateSlippageBps(quote.getRate(), currentRate.getRate());
        if (slippageBps.compareTo(MAX_SLIPPAGE_BPS) > 0) {
            recordFailedExchangeUseCase.execute(quote, currentRate.getRate(), slippageBps);
            throw new ExchangeSlippageExceededException(slippageBps, MAX_SLIPPAGE_BPS);
        }

        Wallet sourceWallet = walletRepository.findByUserIdAndCurrency(userId, quote.getFromCurrency())
                .orElseThrow(() -> {
                    recordFailedExchangeUseCase.execute(quote, currentRate.getRate(), slippageBps);
                    return new IllegalArgumentException("You don't have a " + quote.getFromCurrency() + " wallet");
                });

        if (sourceWallet.getStatus() != WalletStatus.ACTIVE) {
            recordFailedExchangeUseCase.execute(quote, currentRate.getRate(), slippageBps);
            throw new IllegalStateException("Source wallet is not active");
        }

        Wallet destinationWallet = walletRepository.findByUserIdAndCurrency(userId, quote.getToCurrency())
                .orElseGet(() -> walletRepository.save(new Wallet(userId, quote.getToCurrency())));

        if (destinationWallet.getStatus() != WalletStatus.ACTIVE) {
            recordFailedExchangeUseCase.execute(quote, currentRate.getRate(), slippageBps);
            throw new IllegalStateException("Destination wallet is not active");
        }

        UUID liquiditySourceWalletId = SystemWallets.getLiquidityWallet(quote.getFromCurrency());
        UUID liquidityDestinationWalletId = SystemWallets.getLiquidityWallet(quote.getToCurrency());
        UUID feeWalletId = SystemWallets.getFeeWallet(quote.getFromCurrency());

        UUID transactionId;
        try {
            transactionId = recordLedgerEntryUseCase.recordExchange(
                    sourceWallet.getId(),
                    destinationWallet.getId(),
                    liquiditySourceWalletId,
                    liquidityDestinationWalletId,
                    feeWalletId,
                    quote.getFromAmount(),
                    quote.getToAmount(),
                    quote.getFeeAmount(),
                    quote.getId(),
                    "Currency exchange " + quote.getFromCurrency() + " -> " + quote.getToCurrency(),
                    "تصريف عملات " + quote.getFromCurrency() + " -> " + quote.getToCurrency());
        } catch (RuntimeException ex) {
            recordFailedExchangeUseCase.execute(quote, currentRate.getRate(), slippageBps);
            throw ex;
        }

        quote.markExecuted();
        exchangeQuoteRepository.save(quote);

        Exchange exchange = new Exchange(
                quote.getId(),
                userId,
                quote.getFromCurrency(),
                quote.getToCurrency(),
                quote.getFromAmount(),
                quote.getToAmount(),
                quote.getRate(),
                currentRate.getRate(),
                slippageBps,
                quote.getFeeAmount(),
                quote.getTotalDeducted(),
                transactionId);

        Exchange savedExchange = exchangeRepository.save(exchange);
        return toResponse(savedExchange);
    }

    private BigDecimal calculateSlippageBps(BigDecimal quoteRate, BigDecimal currentRate) {
        BigDecimal rateDiff = currentRate.subtract(quoteRate).abs();
        return rateDiff
                .divide(quoteRate, RATE_SCALE, RoundingMode.HALF_UP)
                .multiply(BPS_MULTIPLIER)
                .setScale(BPS_SCALE, RoundingMode.HALF_UP);
    }

    private ExecuteExchangeResponse toResponse(Exchange exchange) {
        return new ExecuteExchangeResponse(
                exchange.getId(),
                exchange.getReferenceNo(),
                exchange.getFromCurrency(),
                exchange.getToCurrency(),
                exchange.getFromAmount(),
                exchange.getToAmount(),
                exchange.getRateAtQuote(),
                exchange.getRateAtExecute(),
                exchange.getSlippageBps(),
                exchange.getFeeAmount(),
                exchange.getTotalDeducted(),
                exchange.getStatus());
    }
}
