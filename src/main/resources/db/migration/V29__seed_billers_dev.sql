-- Seed 4 Wallets for the Billers (Using SystemWallets format IDs, though they are stored in the wallets table)
-- We will use hardcoded UUIDs for the wallets so we can reference them in the billers table
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
        'c1000000-0000-0000-0000-000000000001',
        NULL,
        'YER',
        0.0000,
        'ACTIVE',
        NOW(),
        NOW()
    ), -- Yemen Mobile Wallet
    (
        'c1000000-0000-0000-0000-000000000002',
        NULL,
        'YER',
        0.0000,
        'ACTIVE',
        NOW(),
        NOW()
    ), -- SabaFon Wallet
    (
        'c1000000-0000-0000-0000-000000000003',
        NULL,
        'YER',
        0.0000,
        'ACTIVE',
        NOW(),
        NOW()
    ), -- Public Electricity Wallet
    (
        'c1000000-0000-0000-0000-000000000004',
        NULL,
        'YER',
        0.0000,
        'ACTIVE',
        NOW(),
        NOW()
    );
-- YemenNet Wallet

-- Seed 4 Billers
INSERT INTO
    billers (
        id,
        code,
        name,
        category,
        supported_currency,
        wallet_id,
        status,
        created_at
    )
VALUES (
        'b1000000-0000-0000-0000-000000000001',
        'YEMEN_MOBILE',
        'Yemen Mobile',
        'TELECOM',
        'YER',
        'c1000000-0000-0000-0000-000000000001',
        'ACTIVE',
        NOW()
    ),
    (
        'b1000000-0000-0000-0000-000000000002',
        'SABAFON',
        'SabaFon',
        'TELECOM',
        'YER',
        'c1000000-0000-0000-0000-000000000002',
        'ACTIVE',
        NOW()
    ),
    (
        'b1000000-0000-0000-0000-000000000003',
        'PEC',
        'Public Electricity Corp',
        'ELECTRICITY',
        'YER',
        'c1000000-0000-0000-0000-000000000003',
        'ACTIVE',
        NOW()
    ),
    (
        'b1000000-0000-0000-0000-000000000004',
        'YEMEN_NET',
        'YemenNet ADSL',
        'INTERNET',
        'YER',
        'c1000000-0000-0000-0000-000000000004',
        'ACTIVE',
        NOW()
    );