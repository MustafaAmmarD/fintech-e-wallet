package com.fintech.ewallet.exchange.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exchanges")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeJpaEntity {

    @Id
    private UUID id;

    @Column(name = "reference_no", nullable = false, unique = true, length = 30)
    private String referenceNo;

    @Column(name = "quote_id", nullable = false)
    private UUID quoteId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "from_currency", nullable = false, length = 3)
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false, length = 3)
    private String toCurrency;

    @Column(name = "from_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal fromAmount;

    @Column(name = "to_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal toAmount;

    @Column(name = "rate_at_quote", nullable = false, precision = 19, scale = 8)
    private BigDecimal rateAtQuote;

    @Column(name = "rate_at_execute", nullable = false, precision = 19, scale = 8)
    private BigDecimal rateAtExecute;

    @Column(name = "slippage_bps", precision = 10, scale = 2)
    private BigDecimal slippageBps;

    @Column(name = "fee_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal feeAmount;

    @Column(name = "total_deducted", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalDeducted;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
