CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    wallet_id UUID NOT NULL REFERENCES wallets (id),
    entry_type VARCHAR(10) NOT NULL CHECK (
        entry_type IN ('DEBIT', 'CREDIT')
    ),
    amount DECIMAL(19, 4) NOT NULL CHECK (amount > 0),
    balance_after DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    reference_type VARCHAR(20) NOT NULL CHECK (
        reference_type IN (
            'TRANSFER',
            'DEPOSIT',
            'WITHDRAWAL',
            'FEE'
        )
    ),
    reference_id UUID NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL
);

-- Index for finding all entries of a wallet (sorted by time)
CREATE INDEX idx_ledger_wallet_created ON ledger_entries (wallet_id, created_at DESC);

-- Index for finding all entries of a transaction
CREATE INDEX idx_ledger_transaction ON ledger_entries (transaction_id);

-- Index for transaction history queries
CREATE INDEX idx_ledger_reference ON ledger_entries (reference_type, reference_id);

-- Prevent accidental updates/deletes (immutability enforcement)
-- Note: This is application-level. In production, also use database triggers or row-level security