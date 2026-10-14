-- A promotion can be used by many rides; keep its foreign key and all rider/driver constraints.
-- Safe to re-run. Back up the database before deployment, then run against riderservice_db.
BEGIN;
SET LOCAL lock_timeout = '5s';
DO $$
DECLARE
    offer_column smallint;
    constraint_name text;
BEGIN
    SELECT attnum INTO STRICT offer_column
    FROM pg_attribute
    WHERE attrelid = 'rides'::regclass AND attname = 'offer_id' AND NOT attisdropped;

    FOR constraint_name IN
        SELECT conname FROM pg_constraint
        WHERE conrelid = 'rides'::regclass AND contype = 'u'
          AND conkey = ARRAY[offer_column]::smallint[]
    LOOP
        EXECUTE format('ALTER TABLE rides DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;
CREATE INDEX IF NOT EXISTS rides_offer_rider_usage_idx ON rides (offer_id, rider_id, current_status);
COMMIT;
