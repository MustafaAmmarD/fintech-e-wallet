CREATE TABLE admin_actions (
    id UUID PRIMARY KEY,
    admin_id UUID NOT NULL REFERENCES users (id),
    action_type VARCHAR(30) NOT NULL, -- FREEZE_WALLET, UNFREEZE_WALLET
    target_type VARCHAR(30) NOT NULL, -- WALLET, USER
    target_id UUID NOT NULL,
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_admin_actions_target ON admin_actions (
    target_type,
    target_id,
    created_at DESC
);