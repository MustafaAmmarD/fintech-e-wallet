package com.fintech.ewallet.bill.application.dto;

import com.fintech.ewallet.bill.domain.BillerCategory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BillExecuteResponse(
        UUID paymentId,
        String referenceNo,
        String billerName,
        BillerCategory billerCategory,
        String customerAccountNumber,
        BigDecimal amount,
        BigDecimal feeAmount,
        BigDecimal totalDeducted,
        String currency,
        String status,
        Instant completedAt) {
}
