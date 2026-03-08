package com.fintech.ewallet.bill.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bill_payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillPaymentJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String referenceNo;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID billerId;

    @Column(nullable = false)
    private String customerAccountNumber;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private BigDecimal feeAmount;

    @Column(nullable = false)
    private BigDecimal totalDeducted;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String status;

    @Column(nullable = true)
    private UUID transactionId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
