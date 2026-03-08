package com.fintech.ewallet.exchange.api;

import com.fintech.ewallet.exchange.application.ExecuteExchangeUseCase;
import com.fintech.ewallet.exchange.application.GetExchangeHistoryUseCase;
import com.fintech.ewallet.exchange.application.GetExchangeQuoteUseCase;
import com.fintech.ewallet.exchange.application.GetExchangeRatesUseCase;
import com.fintech.ewallet.exchange.application.dto.ExecuteExchangeRequest;
import com.fintech.ewallet.exchange.application.dto.ExecuteExchangeResponse;
import com.fintech.ewallet.exchange.application.dto.ExchangeHistoryResponse;
import com.fintech.ewallet.exchange.application.dto.ExchangeQuoteResponse;
import com.fintech.ewallet.exchange.application.dto.ExchangeRateResponse;
import com.fintech.ewallet.exchange.domain.ExchangeRate;
import com.fintech.ewallet.wallet.domain.Currency;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/exchange")
@RequiredArgsConstructor
@Tag(name = "Exchange", description = "Currency exchange operations")
public class ExchangeController {

    private final GetExchangeRatesUseCase getExchangeRatesUseCase;
    private final GetExchangeQuoteUseCase getExchangeQuoteUseCase;
    private final ExecuteExchangeUseCase executeExchangeUseCase;
    private final GetExchangeHistoryUseCase getExchangeHistoryUseCase;

    @GetMapping("/rates")
    @Operation(summary = "Get exchange rates", description = "Returns all configured exchange rates.")
    public ResponseEntity<List<ExchangeRateResponse>> getRates() {
        List<ExchangeRateResponse> response = getExchangeRatesUseCase.execute()
                .stream()
                .map(this::toRateResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/quote")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get exchange quote", description = "Creates a 30-second exchange quote.")
    public ResponseEntity<ExchangeQuoteResponse> getQuote(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @RequestParam("from") Currency fromCurrency,
            @RequestParam("to") Currency toCurrency,
            @RequestParam("amount") @DecimalMin(value = "0.0001", message = "Amount must be positive") BigDecimal amount) {

        ExchangeQuoteResponse response = getExchangeQuoteUseCase.execute(userId, fromCurrency, toCurrency, amount);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/execute")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Execute exchange", description = "Executes a valid quote and posts ledger entries.")
    public ResponseEntity<ExecuteExchangeResponse> execute(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @Parameter(name = "Idempotency-Key", required = true, description = "Unique key to prevent duplicate exchange execution")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ExecuteExchangeRequest request) {

        ExecuteExchangeResponse response = executeExchangeUseCase.execute(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get exchange history", description = "Returns exchange history for the authenticated user.")
    public ResponseEntity<List<ExchangeHistoryResponse>> getHistory(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "20") int limit) {

        List<ExchangeHistoryResponse> response = getExchangeHistoryUseCase.execute(userId, limit);
        return ResponseEntity.ok(response);
    }

    private ExchangeRateResponse toRateResponse(ExchangeRate exchangeRate) {
        return new ExchangeRateResponse(
                exchangeRate.getFromCurrency(),
                exchangeRate.getToCurrency(),
                exchangeRate.getRate(),
                exchangeRate.getEffectiveAt());
    }
}
