-- =============================================================
-- V2: Create Users Table
-- =============================================================

CREATE TABLE users (
    id UUID PRIMARY KEY,
    phone_number VARCHAR(20) NOT NULL,
    country_code VARCHAR(5) NOT NULL DEFAULT 'YE',
    full_name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    kyc_status VARCHAR(20) NOT NULL DEFAULT 'NONE',
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    language VARCHAR(5) NOT NULL DEFAULT 'ar',
    referral_code VARCHAR(20),
    failed_login_attempts INT NOT NULL DEFAULT 0,
    last_login_at TIMESTAMP
    WITH
        TIME ZONE,
        locked_until TIMESTAMP
    WITH
        TIME ZONE,
        created_at TIMESTAMP
    WITH
        TIME ZONE NOT NULL DEFAULT NOW(),
        updated_at TIMESTAMP
    WITH
        TIME ZONE NOT NULL DEFAULT NOW(),
        deleted_at TIMESTAMP
    WITH
        TIME ZONE
);

-- Unique constraints
ALTER TABLE users
ADD CONSTRAINT uq_users_phone_number UNIQUE (phone_number);

ALTER TABLE users
ADD CONSTRAINT uq_users_referral_code UNIQUE (referral_code);

-- Indexes for performance
CREATE INDEX idx_users_phone_number ON users (phone_number);

CREATE INDEX idx_users_referral_code ON users (referral_code);

CREATE INDEX idx_users_email ON users (email) WHERE email IS NOT NULL;