-- Create system wallets for liquidity management and fee collection
-- These wallets are owned by the platform, not users (user_id is NULL)

-- Liquidity Wallets (Float Management)
INSERT INTO
    wallets (
        id,
        user_id,
        currency,
        balance,
        status,
        created_at,
        updated_at
    )
VALUES (
        '00000000-0000-0000-0000-000000000001',
        NULL,
        'YER',
        0.0000,
        'ACTIVE',
        NOW(),
        NOW()
    ),
    (
        '00000000-0000-0000-0000-000000000002',
        NULL,
        'SAR',
        0.0000,
        'ACTIVE',
        NOW(),
        NOW()
    ),
    (
        '00000000-0000-0000-0000-000000000003',
        NULL,
        'USD',
        0.0000,
        'ACTIVE',
        NOW(),
        NOW()
    );

-- Fee Collection Wallets (Revenue)
INSERT INTO
    wallets (
        id,
        user_id,
        currency,
        balance,
        status,
        created_at,
        updated_at
    )
VALUES (
        '00000000-0000-0000-0000-000000000011',
        NULL,
        'YER',
        0.0000,
        'ACTIVE',
        NOW(),
        NOW()
    ),
    (
        '00000000-0000-0000-0000-000000000012',
        NULL,
        'SAR',
        0.0000,
        'ACTIVE',
        NOW(),
        NOW()
    ),
    (
        '00000000-0000-0000-0000-000000000013',
        NULL,
        'USD',
        0.0000,
        'ACTIVE',
        NOW(),
        NOW()
    );

-- Note: In production, these wallets start with 0 balance.
-- For development/testing, you can manually seed them with test funds.