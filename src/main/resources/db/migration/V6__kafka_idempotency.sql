-- ============================================================
-- V6 - Kafka consumer idempotency
-- Tracks processed Kafka event IDs so duplicate deliveries
-- (retries, rebalances) do not fire duplicate notifications.
-- ============================================================

CREATE TABLE IF NOT EXISTS processed_kafka_event (
    event_key   VARCHAR(255) NOT NULL PRIMARY KEY,  -- "<topic>:<applicationId>"
    processed_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- Auto-purge events older than 7 days via a scheduled job.
-- Index supports the cleanup query.
CREATE INDEX IF NOT EXISTS idx_processed_event_ts ON processed_kafka_event(processed_at);
