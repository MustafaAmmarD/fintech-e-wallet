CREATE TABLE wallets (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    currency VARCHAR(3) NOT NULL,
    balance DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE (user_id, currency)
);

CREATE INDEX idx_wallets_user_id ON wallets (user_id);