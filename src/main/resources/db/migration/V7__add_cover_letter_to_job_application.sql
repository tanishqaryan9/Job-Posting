-- V7 - Add cover_letter column to job_application
-- Stores the optional cover letter text submitted by applicants.

ALTER TABLE job_application
    ADD COLUMN IF NOT EXISTS cover_letter TEXT;
