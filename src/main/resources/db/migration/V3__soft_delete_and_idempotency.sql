-- ============================================================
-- V3 - Soft Delete & Idempotency
-- ============================================================

-- Soft delete for users
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP DEFAULT NULL;

-- Soft delete for jobs
ALTER TABLE job ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP DEFAULT NULL;

-- Idempotency: prevent duplicate job applications
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_user_job_application') THEN
        ALTER TABLE job_application ADD CONSTRAINT uq_user_job_application UNIQUE (user_id, job_id);
    END IF;
END $$;

-- Index to speed up soft delete filter queries
CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users(deleted_at);
CREATE INDEX IF NOT EXISTS idx_job_deleted_at   ON job(deleted_at);
