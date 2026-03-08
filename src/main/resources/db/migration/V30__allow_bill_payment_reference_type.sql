-- =============================================================
-- V30: Allow BILL_PAYMENT in ledger_entries reference_type check
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
ADD CONSTRAINT ledger_entries_reference_type_check CHECK (
    reference_type IN (
        'TRANSFER',
        'DEPOSIT',
        'WITHDRAWAL',
        'EXCHANGE',
        'REFERRAL',
        'FEE',
        'BILL_PAYMENT'
    )
);