-- =============================================================
-- V34: Alter transfers table for pending/external transfers
-- =============================================================

-- 1. Drop existing status check constraint
ALTER TABLE transfers DROP CONSTRAINT transfers_status_check;

-- 2. Make sender and recipient fields optional
ALTER TABLE transfers ALTER COLUMN sender_user_id DROP NOT NULL;
ALTER TABLE transfers ALTER COLUMN sender_wallet_id DROP NOT NULL;
ALTER TABLE transfers ALTER COLUMN recipient_user_id DROP NOT NULL;
ALTER TABLE transfers ALTER COLUMN recipient_wallet_id DROP NOT NULL;

-- 3. Add new columns for pending/external scenarios
ALTER TABLE transfers ADD COLUMN sender_phone_number VARCHAR(20);
ALTER TABLE transfers ADD COLUMN target_phone_number VARCHAR(20);
ALTER TABLE transfers ADD COLUMN cancel_reason VARCHAR(255);

-- 4. Re-add status constraint with new states
ALTER TABLE transfers ADD CONSTRAINT transfers_status_check CHECK (
    status IN (
        'COMPLETED',
        'FAILED',
        'REVERSED',
        'PENDING',
        'UNCLAIMED',
        'CANCELLED'
    )
);
