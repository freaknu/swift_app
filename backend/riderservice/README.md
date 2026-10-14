# Rider service

The rider service owns fare estimation, ride creation, ride state transitions,
and ride history. It runs on port `8083`. Use the API gateway on port `8080`
for authenticated requests. OpenAPI is available at `/swagger-ui.html` and
health probes at `/actuator/health` while the service is running.

## API

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/v1/fare/estimate` | Estimate distance, duration, and fare; validate an optional promotion |
| `POST` | `/api/v1/offer/getOffers` | List current eligible ride promotions |
| `GET` | `/api/v1/ridetype/getAllRideTypes?isDriver=false` | List all 15 selectable vehicle categories |
| `POST` | `/api/v1/ride/requestRide` | Request a ride |
| `GET` | `/api/v1/ride/getRideDetail/{rideId}` | Fetch one ride |
| `GET` | `/api/v1/fare/rides/{rideId}/fare` | Fetch the persisted fare |
| `GET` | `/api/v1/ride/getRideHistory/{rideId}` | Fetch ordered status history |
| `GET` | `/api/v1/ride/getRideHistoryPaginated/{riderId}?page=0&size=20` | Fetch rider history |
| `GET` | `/api/v1/ride/riders/{riderId}/rides/active` | Fetch the active ride |
| `POST` | `/api/v1/ride/rides/{rideId}/arrive` | Record driver arrival |
| `POST` | `/api/v1/ride/rides/{rideId}/start` | Start the ride |
| `POST` | `/api/v1/ride/rides/{rideId}/complete` | Complete the ride |
| `POST` | `/api/v1/ride/rides/{rideId}/cancel` | Cancel before start |

Example ride request:

```json
{
  "authUserId": 7,
  "riderId": 101,
  "rideType": "HATCHBACK",
  "route": {
    "pickupLatitude": 12.9716,
    "pickupLongitude": 77.5946,
    "destinationLatitude": 12.9352,
    "destinationLongitude": 77.6245,
    "rideType": "HATCHBACK"
  }
}
```

`authUserId` must match the JWT `userId` claim forwarded as `x-user-id` by
the gateway. `riderId` must match the user profile `data.id` returned by
`GET /api/v1/user/getUserDetails`. The rider service checks both before
creating a ride. Direct rider-side acceptance is disabled; the driver uses
`POST /api/v1/driver/offers/{rideId}/accept` after receiving a targeted offer.
Arrival, start, and completion require the assigned driver's profile ID in
the `driverId` request body and verify it against the caller's auth identity.
The selected `rideType` must be a code from `GET /api/v1/vehicle/types`;
the rider catalog returns the same 15 codes even when the legacy `ride_types`
table has no rows. If `route.rideType` is supplied, it must resolve to the
same vehicle category. The exact code is saved as `rideTypeCode`, returned in
the ride response, and sent to matching. Older marketing codes such as
`STANDARD` remain accepted and map explicitly to a vehicle category.

Example cancellation:

```json
{
  "actorId": 101,
  "actorType": "RIDER",
  "reason": "Changed plans"
}
```

## Ride offers

`POST /api/v1/offer/getOffers` accepts `{"rideTypeCode":"BIKE"}`. Generic
promotions (`ride_type_code IS NULL`) are included for every vehicle. Omit or
set `rideTypeCode` to `null` to browse all vehicle categories. The authenticated
rider's completed rides and current reservations determine first-ride and
per-user eligibility. The server uses the current UTC time for validity;
`startFrom` cannot make an expired promotion valid. Database timestamps and
returned `validFrom` / `validUntil` values are UTC, with an exclusive end time.
Offers below their minimum fare can appear in this list because the list has
no route/fare; their quote explains the required minimum.

Pass the selected offer's numeric `id` as `offerId` in `/api/v1/fare/estimate`.
The response's existing `fare.discount` contains the savings and
`fare.totalFare` contains the amount payable. Percentage discounts use the
rounded gross fare (including its tax); fixed discounts use their configured
amount. Both honour `maximumDiscount` and `minimumFare`, round to two decimal
places, and leave at least INR 1.00 payable for the supported Razorpay flow.
The tax line remains the calculated gross-fare tax. A quote consumes no quota.
An unavailable, expired, exhausted, invalid or wrong-vehicle offer returns a
clear error instead of silently quoting the full fare.

Send the same `offerId` in the ride request and its nested `route`. For older
clients, either location alone is accepted; conflicting values are rejected.
The backend calculates the fare again, checks ownership and eligibility, and
saves the offer association, discount and net payable amount in the same ride
transaction. Client-supplied totals are never trusted. Completion events and
subsequent payment validation use that persisted net amount. Only one offer
can be selected for a ride; the `stackable` metadata does not enable stacking.

A promotion row lock serializes reservations. PostgreSQL limits the lock wait
to three seconds and returns a retryable JSON `503` if the promotion is busy;
that failed transaction consumes no usage. `used_count` is maintained from
rides in `REQUESTED`, `MATCHING`, `DRIVER_ASSIGNED`, `DRIVER_ARRIVED`, `STARTED`
and `COMPLETED`. Both global and per-user limits count these states. A booking
reserves a use; `CANCELLED` or `MATCH_FAILED` releases it in the same transaction,
while a completed ride retains it. `new_user_only` checks whether the rider has
any completed ride. The existing asynchronous user-service offer history is
retained as a notification/history integration; it is not the quota authority.

Before deploying this change against an existing PostgreSQL database, back up
`riderservice_db` and apply
[`20261015_allow_reusable_ride_offers.sql`](deployment/migrations/20261015_allow_reusable_ride_offers.sql)
with `psql -v ON_ERROR_STOP=1`. It idempotently removes only a single-column
`UNIQUE (offer_id)` constraint introduced by the former one-to-one mapping,
keeps the offer foreign key and active-rider/driver uniqueness, and adds a
usage-query index. A promotion can then apply to many rides. Existing rows
and configured credentials are preserved. Seed campaigns separately with
unique `offer_code` values and `INSERT ... ON CONFLICT (offer_code) DO NOTHING`;
re-running a deployment must not reactivate or reset an existing campaign.

The explicit [launch seed](deployment/seeds/20261015_launch_offer.sql) adds SWIFT10 (10% off, maximum INR 30, minimum fare INR 100) and SWIFT20 (INR 20 off, minimum fare INR 150). Each is available to all vehicle types for 90 days from its first insertion, with 1,000 total uses and three uses per rider. Apply the seed once after the new service is ready; it is not run at application startup. Re-running it preserves existing usage and validity dates.

`RideOfferIntegrationTest` covers discount calculations, eligibility,
quote/booking consistency, reusability, cancellations, completion events,
transaction rollback, concurrent global/per-user limits and concurrent
campaign edits. The optional `RideOfferPostgresTest` repeats those tests and
verifies the three-second lock bound on a disposable PostgreSQL database:

```bash
./mvnw -Dtest=RideOfferPostgresTest \
  -Dswift.qa.postgres.url=jdbc:postgresql://127.0.0.1:55432/disposable_offers \
  -Dswift.qa.postgres.username=qa -Dswift.qa.postgres.password=qa test
```

This opt-in suite creates and drops its schema; use a disposable database.

## State and integration guarantees

The state flow is `REQUESTED -> MATCHING -> DRIVER_ASSIGNED -> DRIVER_ARRIVED
-> STARTED -> COMPLETED`. If matching exhausts all drivers, it publishes
`ride.match.failed`; this service records `MATCH_FAILED`, releases the rider's
active slot, and allows a new request. Duplicate and late failure events do
not undo an assigned ride. Cancellation is allowed before `STARTED` by the
owning rider or assigned driver. Transitions lock the ride row, while unique
active rider/driver slots prevent concurrent double booking.
At startup the service idempotently expands older PostgreSQL ride status
CHECK constraints to include `MATCH_FAILED`, preserving existing rows.

## Configuration

Use environment variables instead of committing credentials:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT`
- `KAFKA_BOOTSTRAP_SERVERS`, `RIDE_EVENTS_TOPIC`
- `USER_SERVICE_URL` (default `http://localhost:8082`), `DRIVER_SERVICE_URL`
  (default `http://localhost:8096`) for profile ownership lookups

Fare rates are under `app.fare` in `application.yaml`.

## Run and test

```bash
./mvnw test
./mvnw spring-boot:run
```

The gateway verifies JWTs and replaces client supplied `x-user-id` with the
numeric `userId` claim. Keep the rider service behind the gateway; its
internal profile lookups and request headers assume this trusted boundary.

## Completion outbox recovery

Ride completion and both completion-topic rows commit atomically in the ride
transaction. The dispatcher claims each row with a short committed lease, sends
outside the database transaction, and deletes it only after Kafka acknowledgement.
Unacknowledged events do not hold database locks or prevent unrelated rides from
publishing. Legacy `ride-completed` and canonical `ride.completed` are independent
streams, so a broken legacy topic cannot block the canonical payment trigger.

Delivery attempts, next retry, lease token and quarantine state are durable.
Retries wait 1, 2, 4, 8, 16, 32 and 60 seconds, with at most 8 sends per replay.
Acknowledgement times out after 10 seconds; the 30-second lease recovers a crashed
publisher and stale acknowledgements cannot delete a newer worker's claim. Up to
16 stream heads per instance can be in flight. Invalid JSON/serialization/topic,
oversized records and authorization errors are quarantined immediately; exhausted
transient failures are quarantined too. Exception messages/payloads are excluded
from stored diagnostics. Delivery remains at least once across an ack/commit
crash gap, so completion consumers must remain idempotent.

The additive, idempotent PostgreSQL upgrade is
`src/main/resources/db/upgrades/20261014_outbox_delivery.sql`. It runs automatically
at startup with the repository's current Hibernate-update development setup;
apply the same SQL before production `ddl-auto=validate` startup. No existing
completion payload, properties, YAML or credentials are replaced.

Inspect quarantined rows in `ride_completion_outbox` (or
`JpaRideCompletionOutboxRepository.findByQuarantinedAtIsNotNullOrderByQuarantinedAtAsc`).
After correcting the cause, invoke the operator-only bean method
`RideCompletionOutbox.replayQuarantined(id)`. No public HTTP replay endpoint is
exposed. An offline alternative is to stop dispatcher instances, inspect/correct
the row and replay a specific reviewed row:

```sql
UPDATE ride_completion_outbox
SET quarantined_at = NULL, delivery_attempts = 0,
    next_attempt_at = CURRENT_TIMESTAMP, delivery_token = NULL,
    lease_until = NULL, last_failure_type = NULL
WHERE id = REVIEWED_ROW_ID AND quarantined_at IS NOT NULL;
```

Do not bulk reset pending/leased rows; replay is an explicit at-least-once redelivery.

The opt-in broker regression uses a fresh embedded KRaft Kafka broker and an
isolated H2 database, with the production Kafka serializers/configuration. It
checks rollback visibility, the actual canonical completion key/JSON received by
a consumer, deletion only after acknowledgement, and durable retry after a real
producer cannot reach a fresh closed loopback endpoint. It does not use the
user's running Kafka/PostgreSQL services or any payment provider. Run:

```sh
./mvnw -Dswift.qa.broker=true -Dtest=RideCompletionOutboxBrokerTest test
```
