package com.fintech.ewallet.exchange.application;

import com.fintech.ewallet.exchange.domain.ExchangeRate;
import com.fintech.ewallet.exchange.domain.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetExchangeRatesUseCase {

    private final ExchangeRateRepository exchangeRateRepository;

    public List<ExchangeRate> execute() {
        return exchangeRateRepository.findAll();
    }
}
