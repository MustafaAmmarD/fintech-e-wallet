package com.fintech.ewallet.exchange.infrastructure.persistence;

import com.fintech.ewallet.exchange.domain.QuoteStatus;
import com.fintech.ewallet.wallet.domain.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exchange_quotes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeQuoteJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_currency", nullable = false, length = 3)
    private Currency fromCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_currency", nullable = false, length = 3)
    private Currency toCurrency;

    @Column(name = "from_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal fromAmount;

    @Column(name = "to_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal toAmount;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal rate;

    @Column(name = "fee_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal feeAmount;

    @Column(name = "total_deducted", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalDeducted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuoteStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
