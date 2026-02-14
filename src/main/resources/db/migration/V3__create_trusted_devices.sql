-- V3: Create trusted devices table

CREATE TABLE trusted_devices (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    device_id VARCHAR(255) NOT NULL,
    fingerprint VARCHAR(255) NOT NULL,
    device_name VARCHAR(100),
    user_agent TEXT,
    last_ip_address VARCHAR(45),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    last_used_at TIMESTAMP
    WITH
        TIME ZONE,
        created_at TIMESTAMP
    WITH
        TIME ZONE NOT NULL DEFAULT NOW(),
        UNIQUE (user_id, device_id)
);

CREATE INDEX idx_trusted_devices_user ON trusted_devices (user_id);

CREATE INDEX idx_trusted_devices_fingerprint ON trusted_devices (fingerprint);