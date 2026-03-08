-- =============================================================
-- V18: Seed default fee rules for MVP behavior
-- =============================================================
-- TRANSFER: 2% with min 1.00 and max 500.00
-- EXCHANGE: 1% with no min/max cap

INSERT INTO fee_rules (id, operation_type, currency, fee_type, rate, flat_amount, min_amount, max_amount, min_fee, max_fee, is_active)
VALUES
    ('11111111-0000-0000-0000-000000000001', 'TRANSFER', 'YER', 'PERCENTAGE', 0.02000000, NULL, 0.0000, NULL, 1.0000, 500.0000, TRUE),
    ('11111111-0000-0000-0000-000000000002', 'TRANSFER', 'SAR', 'PERCENTAGE', 0.02000000, NULL, 0.0000, NULL, 1.0000, 500.0000, TRUE),
    ('11111111-0000-0000-0000-000000000003', 'TRANSFER', 'USD', 'PERCENTAGE', 0.02000000, NULL, 0.0000, NULL, 1.0000, 500.0000, TRUE),
    ('22222222-0000-0000-0000-000000000001', 'EXCHANGE', 'YER', 'PERCENTAGE', 0.01000000, NULL, 0.0000, NULL, 0.0000, NULL, TRUE),
    ('22222222-0000-0000-0000-000000000002', 'EXCHANGE', 'SAR', 'PERCENTAGE', 0.01000000, NULL, 0.0000, NULL, 0.0000, NULL, TRUE),
    ('22222222-0000-0000-0000-000000000003', 'EXCHANGE', 'USD', 'PERCENTAGE', 0.01000000, NULL, 0.0000, NULL, 0.0000, NULL, TRUE);
