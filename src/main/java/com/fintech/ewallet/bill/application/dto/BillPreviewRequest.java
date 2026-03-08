package com.fintech.ewallet.bill.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record BillPreviewRequest(
        @NotBlank(message = "Biller code is required") String billerCode,

        @NotBlank(message = "Customer account number is required") String customerAccountNumber,

        @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Amount must be strictly positive") BigDecimal amount,

        @NotBlank(message = "Currency is required") String currency) {
}
