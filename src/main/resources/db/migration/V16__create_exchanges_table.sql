-- =============================================================
-- V16: Create exchanges table
-- =============================================================

CREATE TABLE exchanges
(
    id              UUID PRIMARY KEY,
    reference_no    VARCHAR(30)    NOT NULL UNIQUE,
    quote_id        UUID           NOT NULL REFERENCES exchange_quotes (id),
    user_id         UUID           NOT NULL REFERENCES users (id),
    from_currency   VARCHAR(3)     NOT NULL,
    to_currency     VARCHAR(3)     NOT NULL,
    from_amount     NUMERIC(19, 4) NOT NULL CHECK (from_amount > 0),
    to_amount       NUMERIC(19, 4) NOT NULL CHECK (to_amount > 0),
    rate_at_quote   NUMERIC(19, 8) NOT NULL CHECK (rate_at_quote > 0),
    rate_at_execute NUMERIC(19, 8) NOT NULL CHECK (rate_at_execute > 0),
    slippage_bps    NUMERIC(10, 2) CHECK (slippage_bps IS NULL OR slippage_bps >= 0),
    fee_amount      NUMERIC(19, 4) NOT NULL CHECK (fee_amount >= 0),
    total_deducted  NUMERIC(19, 4) NOT NULL CHECK (total_deducted > 0),
    status          VARCHAR(20)    NOT NULL DEFAULT 'COMPLETED'
        CHECK (status IN ('COMPLETED', 'FAILED')),
    transaction_id  UUID,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT exchanges_currency_pair_check CHECK (from_currency <> to_currency)
);

CREATE INDEX idx_exchanges_user ON exchanges (user_id, created_at DESC);
