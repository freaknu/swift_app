-- Run once against the existing authentication PostgreSQL database before deploying
-- the auth-service update. Reapplying is safe; existing token values are preserved.
-- No credentials, database names or connection settings are embedded in this file.
BEGIN;
SET LOCAL lock_timeout = '5s';
ALTER TABLE user_credentials
    ALTER COLUMN access_token TYPE TEXT,
    ALTER COLUMN refresh_token TYPE TEXT;
COMMIT;
