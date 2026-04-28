-- V8: add salary_period column to job table
-- Stores the pay period chosen by the creator: hour | day | month | year
-- Nullable so existing rows remain valid (they default to null = unknown, displayed as-is)
ALTER TABLE job ADD COLUMN IF NOT EXISTS salary_period VARCHAR(10);
