package com.fintech.ewallet.bill.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BillHistoryResponse(
        UUID paymentId,
        String referenceNo,
        String billerName,
        String customerAccountNumber,
        BigDecimal amount,
        BigDecimal feeAmount,
        BigDecimal totalDeducted,
        String currency,
        String status,
        Instant createdAt) {
}
