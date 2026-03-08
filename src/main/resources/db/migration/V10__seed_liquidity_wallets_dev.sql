-- ============================================================
-- V10: Dev/Test Seed Data — Liquidity Wallet Funds
-- ============================================================
-- Seeds liquidity wallets with test funds for development.
-- In production, real money enters through bank/agent integrations.
-- This migration is safe to run in any environment — it uses
-- an UPDATE so it won't create duplicates.
-- ============================================================

UPDATE wallets
SET
    balance = 1000000.0000,
    updated_at = NOW()
WHERE
    id IN (
        '00000000-0000-0000-0000-000000000001', -- LIQUIDITY_YER
        '00000000-0000-0000-0000-000000000002', -- LIQUIDITY_SAR
        '00000000-0000-0000-0000-000000000003' -- LIQUIDITY_USD
    );