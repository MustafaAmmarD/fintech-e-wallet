-- =============================================================
-- V17: Create fee_rules table
-- =============================================================

CREATE TABLE fee_rules
(
    id             UUID PRIMARY KEY,
    operation_type VARCHAR(20)    NOT NULL
        CHECK (operation_type IN ('TRANSFER', 'EXCHANGE', 'DEPOSIT', 'WITHDRAWAL')),
    currency       VARCHAR(3)     NOT NULL
        CHECK (currency IN ('YER', 'SAR', 'USD')),
    fee_type       VARCHAR(20)    NOT NULL
        CHECK (fee_type IN ('PERCENTAGE', 'FLAT')),
    rate           NUMERIC(12, 8),
    flat_amount    NUMERIC(19, 4),
    min_amount     NUMERIC(19, 4) NOT NULL DEFAULT 0
        CHECK (min_amount >= 0),
    max_amount     NUMERIC(19, 4),
    min_fee        NUMERIC(19, 4) NOT NULL DEFAULT 0
        CHECK (min_fee >= 0),
    max_fee        NUMERIC(19, 4),
    is_active      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT fee_rules_range_check CHECK (max_amount IS NULL OR max_amount > min_amount),
    CONSTRAINT fee_rules_fee_cap_check CHECK (max_fee IS NULL OR max_fee >= min_fee),
    CONSTRAINT fee_rules_type_fields_check CHECK (
        (fee_type = 'PERCENTAGE' AND rate IS NOT NULL AND rate >= 0 AND rate <= 1 AND flat_amount IS NULL)
            OR
        (fee_type = 'FLAT' AND flat_amount IS NOT NULL AND flat_amount >= 0 AND rate IS NULL)),
    CONSTRAINT fee_rules_operation_currency_amount_unique UNIQUE (operation_type, currency, min_amount, max_amount)
);

CREATE INDEX idx_fee_rules_lookup
    ON fee_rules (operation_type, currency, is_active, min_amount);
