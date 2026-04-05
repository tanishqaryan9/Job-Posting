-- ============================================================
-- V2 - Performance Indexes
-- ============================================================

-- Speed up JWT auth lookup (most frequent query)
CREATE INDEX IF NOT EXISTS idx_app_user_username ON app_user(username);

-- Speed up OAuth2 login lookup
CREATE INDEX IF NOT EXISTS idx_app_user_provider ON app_user(provider_id, provider_type);

-- Speed up notification queries by user
CREATE INDEX IF NOT EXISTS idx_notification_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notification_user_read ON notifications(user_id, is_read);

-- Speed up application queries
CREATE INDEX IF NOT EXISTS idx_job_application_job_id  ON job_application(job_id);
CREATE INDEX IF NOT EXISTS idx_job_application_user_id ON job_application(user_id);

-- Speed up job salary range binary search queries
CREATE INDEX IF NOT EXISTS idx_job_salary ON job(salary);

-- Speed up refresh token lookup
CREATE INDEX IF NOT EXISTS idx_refresh_token_token ON refresh_token(token);
CREATE INDEX IF NOT EXISTS idx_refresh_token_user  ON refresh_token(app_user_id);
