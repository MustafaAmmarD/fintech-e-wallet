CREATE TABLE bill_payments (
    id UUID PRIMARY KEY,
    reference_no VARCHAR(30) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users (id),
    biller_id UUID NOT NULL REFERENCES billers (id),
    customer_account_number VARCHAR(50) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    fee_amount DECIMAL(19, 4) NOT NULL DEFAULT 0,
    total_deducted DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    transaction_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bill_payments_user ON bill_payments (user_id, created_at DESC);