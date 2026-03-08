-- =============================================================
-- V14: Create exchange_rates table and allow EXCHANGE reference type
-- =============================================================

DO $$
DECLARE
    check_name TEXT;
BEGIN
    FOR check_name IN
        SELECT con.conname
        FROM pg_constraint con
                 JOIN pg_class rel ON rel.oid = con.conrelid
        WHERE rel.relname = 'ledger_entries'
          AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) ILIKE '%reference_type%'
        LOOP
            EXECUTE format('ALTER TABLE ledger_entries DROP CONSTRAINT %I', check_name);
        END LOOP;
END
$$;

ALTER TABLE ledger_entries
    ADD CONSTRAINT ledger_entries_reference_type_check
        CHECK (reference_type IN ('TRANSFER', 'DEPOSIT', 'WITHDRAWAL', 'EXCHANGE', 'FEE'));

CREATE TABLE exchange_rates
(
    id            UUID PRIMARY KEY,
    from_currency VARCHAR(3)     NOT NULL,
    to_currency   VARCHAR(3)     NOT NULL,
    rate          NUMERIC(19, 8) NOT NULL CHECK (rate > 0),
    set_by        UUID REFERENCES users (id),
    effective_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT exchange_rates_currency_pair_check CHECK (from_currency <> to_currency),
    CONSTRAINT exchange_rates_pair_unique UNIQUE (from_currency, to_currency)
);
