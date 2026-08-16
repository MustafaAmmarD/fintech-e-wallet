package com.fintech.ewallet.wallet.application.dto;

import com.fintech.ewallet.wallet.domain.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CancelTransferRequest(
        @NotBlank(message = "Transfer number is required")
        String transferNumber,
        
        @NotBlank(message = "Process number is required")
        String processNumber,
        
        @NotNull(message = "Currency is required")
        Currency currency,
        
        @NotBlank(message = "Reason is required")
        String reason
) {}
