-- =============================================================
-- V8: Create transfers table for P2P transfer records
-- =============================================================

CREATE TABLE transfers (
    id UUID PRIMARY KEY,
    reference_no VARCHAR(20) NOT NULL UNIQUE,
    sender_user_id UUID NOT NULL REFERENCES users (id),
    sender_wallet_id UUID NOT NULL REFERENCES wallets (id),
    recipient_user_id UUID NOT NULL REFERENCES users (id),
    recipient_wallet_id UUID NOT NULL REFERENCES wallets (id),
    amount DECIMAL(19, 4) NOT NULL CHECK (amount > 0),
    fee_amount DECIMAL(19, 4) NOT NULL DEFAULT 0,
    total_deducted DECIMAL(19, 4) NOT NULL CHECK (total_deducted > 0),
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (
        status IN (
            'COMPLETED',
            'FAILED',
            'REVERSED'
        )
    ),
    description VARCHAR(500),
    transaction_id UUID,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);

-- Index for sender's transfer history
CREATE INDEX idx_transfers_sender ON transfers (
    sender_user_id,
    created_at DESC
);

-- Index for recipient's transfer history
CREATE INDEX idx_transfers_recipient ON transfers (
    recipient_user_id,
    created_at DESC
);

-- Index for reference number lookup
CREATE INDEX idx_transfers_reference ON transfers (reference_no);

-- Index for linking to ledger entries
CREATE INDEX idx_transfers_transaction ON transfers (transaction_id);