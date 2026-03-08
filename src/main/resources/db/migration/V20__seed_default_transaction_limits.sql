-- V20: Seed default transaction limits for MVP limits engine

WITH tiers AS (
    SELECT 'BASIC'::VARCHAR AS user_tier
    UNION ALL
    SELECT 'VERIFIED'::VARCHAR
),
operations AS (
    SELECT 'TRANSFER'::VARCHAR AS operation_type
    UNION ALL
    SELECT 'WITHDRAWAL'::VARCHAR
    UNION ALL
    SELECT 'EXCHANGE'::VARCHAR
),
currencies AS (
    SELECT 'YER'::VARCHAR AS currency
    UNION ALL
    SELECT 'SAR'::VARCHAR
    UNION ALL
    SELECT 'USD'::VARCHAR
),
limit_templates AS (
    SELECT
        t.user_tier,
        o.operation_type,
        c.currency,
        l.limit_type,
        CASE
            WHEN l.limit_type = 'PER_TRANSACTION' THEN 100000::NUMERIC
            WHEN l.limit_type = 'DAILY' AND t.user_tier = 'VERIFIED' THEN 500000::NUMERIC
            WHEN l.limit_type = 'DAILY' AND t.user_tier = 'BASIC' THEN 10000::NUMERIC
            WHEN l.limit_type = 'MONTHLY' AND t.user_tier = 'VERIFIED' THEN 15000000::NUMERIC
            WHEN l.limit_type = 'MONTHLY' AND t.user_tier = 'BASIC' THEN 300000::NUMERIC
            ELSE NULL
        END AS max_amount,
        CASE
            WHEN l.limit_type = 'VELOCITY' THEN 1
            ELSE NULL
        END AS window_hours,
        CASE
            WHEN l.limit_type = 'VELOCITY' AND t.user_tier = 'VERIFIED' THEN 20
            WHEN l.limit_type = 'VELOCITY' AND t.user_tier = 'BASIC' THEN 10
            ELSE NULL
        END AS max_count
    FROM tiers t
    CROSS JOIN operations o
    CROSS JOIN currencies c
    CROSS JOIN (
        SELECT 'PER_TRANSACTION'::VARCHAR AS limit_type
        UNION ALL
        SELECT 'DAILY'::VARCHAR
        UNION ALL
        SELECT 'MONTHLY'::VARCHAR
        UNION ALL
        SELECT 'VELOCITY'::VARCHAR
    ) l
),
rows_with_hash AS (
    SELECT
        user_tier,
        operation_type,
        currency,
        limit_type,
        max_amount,
        window_hours,
        max_count,
        md5(user_tier || ':' || operation_type || ':' || currency || ':' || limit_type) AS hash_value
    FROM limit_templates
)
INSERT INTO transaction_limits (
    id,
    user_tier,
    operation_type,
    currency,
    limit_type,
    max_amount,
    window_hours,
    max_count,
    is_active
)
SELECT
    (
        substr(hash_value, 1, 8) || '-' ||
        substr(hash_value, 9, 4) || '-' ||
        substr(hash_value, 13, 4) || '-' ||
        substr(hash_value, 17, 4) || '-' ||
        substr(hash_value, 21, 12)
    )::UUID AS id,
    user_tier,
    operation_type,
    currency,
    limit_type,
    max_amount,
    window_hours,
    max_count,
    TRUE
FROM rows_with_hash
ON CONFLICT DO NOTHING;
