-- Additive/idempotent upgrade for existing PostgreSQL development databases.
-- Apply before boot when using production ddl-auto=validate.
ALTER TABLE ride_completion_outbox ADD COLUMN IF NOT EXISTS delivery_attempts integer DEFAULT 0;
UPDATE ride_completion_outbox SET delivery_attempts = 0 WHERE delivery_attempts IS NULL;
ALTER TABLE ride_completion_outbox ALTER COLUMN delivery_attempts SET DEFAULT 0;
ALTER TABLE ride_completion_outbox ALTER COLUMN delivery_attempts SET NOT NULL;
ALTER TABLE ride_completion_outbox ADD COLUMN IF NOT EXISTS next_attempt_at timestamp with time zone;
ALTER TABLE ride_completion_outbox ADD COLUMN IF NOT EXISTS lease_until timestamp with time zone;
ALTER TABLE ride_completion_outbox ADD COLUMN IF NOT EXISTS delivery_token varchar(36);
ALTER TABLE ride_completion_outbox ADD COLUMN IF NOT EXISTS quarantined_at timestamp with time zone;
ALTER TABLE ride_completion_outbox ADD COLUMN IF NOT EXISTS last_failure_type varchar(160);
CREATE INDEX IF NOT EXISTS idx_ride_completion_outbox_due
    ON ride_completion_outbox (quarantined_at, next_attempt_at, lease_until);
CREATE INDEX IF NOT EXISTS idx_completion_outbox_stream ON ride_completion_outbox (topic, event_key, id);
