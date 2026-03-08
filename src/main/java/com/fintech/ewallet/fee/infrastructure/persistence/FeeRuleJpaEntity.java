package com.fintech.ewallet.fee.infrastructure.persistence;

import com.fintech.ewallet.fee.domain.FeeOperation;
import com.fintech.ewallet.fee.domain.FeeType;
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
@Table(name = "fee_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeRuleJpaEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20)
    private FeeOperation operationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false, length = 20)
    private FeeType feeType;

    @Column(precision = 12, scale = 8)
    private BigDecimal rate;

    @Column(name = "flat_amount", precision = 19, scale = 4)
    private BigDecimal flatAmount;

    @Column(name = "min_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal minAmount;

    @Column(name = "max_amount", precision = 19, scale = 4)
    private BigDecimal maxAmount;

    @Column(name = "min_fee", nullable = false, precision = 19, scale = 4)
    private BigDecimal minFee;

    @Column(name = "max_fee", precision = 19, scale = 4)
    private BigDecimal maxFee;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
