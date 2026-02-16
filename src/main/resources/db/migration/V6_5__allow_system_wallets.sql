-- Modify wallets table to allow NULL user_id for system wallets
ALTER TABLE wallets ALTER COLUMN user_id DROP NOT NULL;

-- Update the unique constraint to allow system wallets (NULL user_id)
-- Drop the old constraint
ALTER TABLE wallets
DROP CONSTRAINT IF EXISTS wallets_user_id_currency_key;

-- Add new constraint: UNIQUE for user wallets, but allow multiple system wallets
-- System wallets will have user_id = NULL, so they won't conflict with this constraint
ALTER TABLE wallets
ADD CONSTRAINT wallets_user_currency_unique UNIQUE (user_id, currency);

-- Note: This allows NULL user_id (for system wallets) while maintaining
-- the constraint that each user can only have 1 wallet per currency