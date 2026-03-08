package com.fintech.ewallet.bill.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillPayment {
    private UUID id;
    private String referenceNo;
    private UUID userId;
    private UUID billerId;
    private String customerAccountNumber;
    private BigDecimal amount;
    private BigDecimal feeAmount;
    private BigDecimal totalDeducted;
    private String currency;
    private String status; // COMPLETED, FAILED
    private UUID transactionId;
    private Instant createdAt;
}
