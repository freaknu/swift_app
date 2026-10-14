-- Apply before boot when production uses ddl-auto=validate. Existing IntelliJ update mode adds these columns automatically.
ALTER TABLE ride_payments ADD COLUMN IF NOT EXISTS order_creation_attempted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE ride_payments ADD COLUMN IF NOT EXISTS provider_operation_token VARCHAR(36);
ALTER TABLE ride_payments ADD COLUMN IF NOT EXISTS provider_operation_expires_at TIMESTAMP WITH TIME ZONE;
-- No DEFAULT/backfill: NULL distinguishes old rows with unknown provider POST history from new version-1 rows.
ALTER TABLE ride_payments ADD COLUMN IF NOT EXISTS provider_workflow_version INTEGER;

-- Before upgrading a live deployment, reconcile legacy PENDING payments whose Razorpay request may have
-- been accepted before a crash. Older rows remain NULL workflow version and cannot POST when lookup is empty.
-- Older versions did not persist a POST-attempt marker. Do not clear an
-- attempted marker solely because a receipt lookup is empty: an uncertain provider outcome is not a
-- definitive rejection. A successfully recovered order keeps the original receipt and order identity.
