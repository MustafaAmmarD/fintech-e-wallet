-- =============================================================
-- V15: Create exchange_quotes table
-- =============================================================

CREATE TABLE exchange_quotes
(
    id             UUID PRIMARY KEY,
    user_id        UUID            NOT NULL REFERENCES users (id),
    from_currency  VARCHAR(3)      NOT NULL,
    to_currency    VARCHAR(3)      NOT NULL,
    from_amount    NUMERIC(19, 4)  NOT NULL CHECK (from_amount > 0),
    to_amount      NUMERIC(19, 4)  NOT NULL CHECK (to_amount > 0),
    rate           NUMERIC(19, 8)  NOT NULL CHECK (rate > 0),
    fee_amount     NUMERIC(19, 4)  NOT NULL CHECK (fee_amount >= 0),
    total_deducted NUMERIC(19, 4)  NOT NULL CHECK (total_deducted > 0),
    status         VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'EXECUTED', 'EXPIRED')),
    expires_at     TIMESTAMPTZ     NOT NULL,
    created_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT exchange_quotes_currency_pair_check CHECK (from_currency <> to_currency)
);

CREATE INDEX idx_quotes_user ON exchange_quotes (user_id, created_at DESC);
CREATE INDEX idx_quotes_status ON exchange_quotes (status, expires_at);
