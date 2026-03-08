-- V22: Add privacy setting to control whether full name is visible to others

ALTER TABLE users
    ADD COLUMN show_full_name BOOLEAN NOT NULL DEFAULT TRUE;
