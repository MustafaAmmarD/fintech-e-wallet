package com.fintech.ewallet.exchange.application;

import com.fintech.ewallet.exchange.application.dto.ExchangeHistoryResponse;
import com.fintech.ewallet.exchange.domain.Exchange;
import com.fintech.ewallet.exchange.domain.ExchangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetExchangeHistoryUseCase {

    private final ExchangeRepository exchangeRepository;

    public List<ExchangeHistoryResponse> execute(UUID userId, int limit) {
        int clampedLimit = Math.min(Math.max(limit, 1), 50);
        return exchangeRepository.findByUserIdOrderByCreatedAtDesc(userId, clampedLimit)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ExchangeHistoryResponse toResponse(Exchange exchange) {
        return new ExchangeHistoryResponse(
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
                exchange.getStatus(),
                exchange.getCreatedAt());
    }
}
