package com.fintech.ewallet.bill.application.dto;

import com.fintech.ewallet.bill.domain.BillerCategory;
import java.math.BigDecimal;

public record BillPreviewResponse(
        String billerName,
        BillerCategory billerCategory,
        String customerAccountNumber,
        BigDecimal amount,
        BigDecimal feeAmount,
        BigDecimal totalDeducted,
        String currency,
        BigDecimal senderBalanceAfter) {
}
