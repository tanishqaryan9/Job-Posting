-- ============================================================
-- V5 - Add updated_at to users, role column to app_user
-- ============================================================

-- Users entity has @UpdateTimestamp on updated_at but V1 never added the column
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NULL;

-- AppUser now carries a role string for @PreAuthorize checks.
-- Existing rows default to ROLE_USER.
ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER';
