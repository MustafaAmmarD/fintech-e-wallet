-- V24: Create referrals table for code-based referral program

CREATE TABLE referrals (
    id UUID PRIMARY KEY,
    referrer_id UUID NOT NULL REFERENCES users(id),
    referee_id UUID NOT NULL REFERENCES users(id),
    referral_code VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    referrer_reward NUMERIC(19,4),
    referee_reward NUMERIC(19,4),
    rewarded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_referrals_referee UNIQUE (referee_id),
    CONSTRAINT chk_referrals_self_referral CHECK (referrer_id <> referee_id)
);

CREATE INDEX idx_referrals_referrer
    ON referrals(referrer_id, created_at DESC);

CREATE INDEX idx_referrals_status
    ON referrals(status);
