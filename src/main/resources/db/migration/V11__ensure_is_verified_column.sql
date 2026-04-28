-- V10 may not have applied on this DB instance.
-- This migration ensures is_verified exists regardless.
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_verified BOOLEAN NOT NULL DEFAULT FALSE;