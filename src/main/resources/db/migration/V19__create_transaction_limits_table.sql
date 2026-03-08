-- V19: Create transaction_limits table for configurable limits engine

CREATE TABLE transaction_limits (
    id UUID PRIMARY KEY,
    user_tier VARCHAR(20) NOT NULL,
    operation_type VARCHAR(30) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    limit_type VARCHAR(30) NOT NULL,
    max_amount NUMERIC(19, 4),
    window_hours INTEGER,
    max_count INTEGER,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transaction_limits_lookup
    ON transaction_limits(user_tier, operation_type, currency, is_active);

CREATE UNIQUE INDEX uq_transaction_limits_active
    ON transaction_limits(user_tier, operation_type, currency, limit_type)
    WHERE is_active = TRUE;
