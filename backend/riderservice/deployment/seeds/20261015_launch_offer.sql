-- Explicit launch campaign, not an automatic application-startup seed.
-- Re-running this file never resets usage or extends an existing campaign.
BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL TIME ZONE 'UTC';

INSERT INTO offers (
    offer_code, title, description, ride_type_code, discount_type,
    discount_value, maximum_discount, minimum_fare, valid_from, valid_until,
    is_active, usage_limit, per_user_limit, used_count, new_user_only,
    stackable, created_at, updated_at
) VALUES (
    'SWIFT10', 'Save 10% on your ride',
    '10% off fares from INR 100, up to INR 30. Three uses per rider; one offer per ride.',
    NULL, 'PERCENTAGE', 10.00, 30.00, 100.00,
    CURRENT_TIMESTAMP AT TIME ZONE 'UTC',
    (CURRENT_TIMESTAMP AT TIME ZONE 'UTC') + INTERVAL '90 days',
    TRUE, 1000, 3, 0, FALSE, FALSE,
    CURRENT_TIMESTAMP AT TIME ZONE 'UTC', CURRENT_TIMESTAMP AT TIME ZONE 'UTC'
), (
    'SWIFT20', 'Save INR 20 on your ride',
    'INR 20 off fares from INR 150. Three uses per rider; one offer per ride.',
    NULL, 'FIXED_AMOUNT', 20.00, 20.00, 150.00,
    CURRENT_TIMESTAMP AT TIME ZONE 'UTC',
    (CURRENT_TIMESTAMP AT TIME ZONE 'UTC') + INTERVAL '90 days',
    TRUE, 1000, 3, 0, FALSE, FALSE,
    CURRENT_TIMESTAMP AT TIME ZONE 'UTC', CURRENT_TIMESTAMP AT TIME ZONE 'UTC'
)
ON CONFLICT (offer_code) DO NOTHING;

COMMIT;
