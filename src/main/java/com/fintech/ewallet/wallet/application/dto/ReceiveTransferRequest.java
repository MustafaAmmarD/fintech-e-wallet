package com.fintech.ewallet.wallet.application.dto;

import jakarta.validation.constraints.NotBlank;

public record ReceiveTransferRequest(
        @NotBlank(message = "Transfer number is required")
        String transferNumber,
        
        @NotBlank(message = "Process number is required")
        String processNumber
) {}
