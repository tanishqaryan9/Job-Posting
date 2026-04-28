-- V10: add verified column to users table
-- Tracks whether a user has completed email/phone OTP verification
-- Defaults to false; set to true after successful OTP verification
ALTER TABLE users ADD COLUMN IF NOT EXISTS verified BOOLEAN NOT NULL DEFAULT FALSE;
