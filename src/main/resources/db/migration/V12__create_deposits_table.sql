-- =============================================================
-- V12: Create deposits table for agent cash-in operations
-- =============================================================

CREATE TABLE deposits (
    id UUID PRIMARY KEY,
    reference_no VARCHAR(30) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users (id),
    agent_id UUID NOT NULL REFERENCES users (id),
    wallet_id UUID NOT NULL REFERENCES wallets (id),
    amount DECIMAL(19, 4) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED' CHECK (status IN ('COMPLETED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_deposits_user ON deposits (
    user_id,
    created_at DESC
);

CREATE INDEX idx_deposits_agent ON deposits (
    agent_id,
    created_at DESC
);
