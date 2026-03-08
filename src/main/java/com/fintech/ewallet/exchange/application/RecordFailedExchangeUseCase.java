package com.fintech.ewallet.exchange.application;

import com.fintech.ewallet.exchange.domain.Exchange;
import com.fintech.ewallet.exchange.domain.ExchangeQuote;
import com.fintech.ewallet.exchange.domain.ExchangeRepository;
import com.fintech.ewallet.exchange.domain.ExchangeStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordFailedExchangeUseCase {

    private final ExchangeRepository exchangeRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(ExchangeQuote quote, BigDecimal rateAtExecute, BigDecimal slippageBps) {
        BigDecimal safeRateAtExecute = rateAtExecute != null ? rateAtExecute : quote.getRate();

        Exchange failedExchange = new Exchange(
                quote.getId(),
                quote.getUserId(),
                quote.getFromCurrency(),
                quote.getToCurrency(),
                quote.getFromAmount(),
                quote.getToAmount(),
                quote.getRate(),
                safeRateAtExecute,
                slippageBps,
                quote.getFeeAmount(),
                quote.getTotalDeducted(),
                ExchangeStatus.FAILED,
                null);

        exchangeRepository.save(failedExchange);
        log.warn("Recorded failed exchange attempt. quoteId={}, userId={}", quote.getId(), quote.getUserId());
    }
}
