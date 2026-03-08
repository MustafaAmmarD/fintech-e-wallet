package com.fintech.ewallet.exchange.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ExecuteExchangeRequest(
        @NotNull(message = "Quote ID is required") UUID quoteId) {
}
