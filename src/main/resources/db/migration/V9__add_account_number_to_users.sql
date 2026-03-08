-- =============================================================
-- V9: Add account_number column to users table
-- =============================================================
-- Account numbers are Luhn-validated 9-digit numbers used for
-- recipient lookup during P2P transfers.

ALTER TABLE users ADD COLUMN account_number VARCHAR(15);

-- Create unique index for account number lookups
CREATE UNIQUE INDEX idx_users_account_number ON users (account_number)
WHERE
    account_number IS NOT NULL;

-- Note: account_number is nullable to support existing users.
-- The application will generate account numbers for new registrations.
-- Existing users will get account numbers via a backfill migration or
-- on their next login.