-- ============================================================
-- V4 - Link AppUser to User Profile
-- Adds user_profile_id FK on app_user table
-- ============================================================

ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS user_profile_id BIGINT
    REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_app_user_profile_id ON app_user(user_profile_id);