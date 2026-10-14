# Rider service

The rider service owns fare estimation, ride creation, ride state transitions,
and ride history. It runs on port `8083`. Use the API gateway on port `8080`
for authenticated requests. OpenAPI is available at `/swagger-ui.html` and
health probes at `/actuator/health` while the service is running.

## API

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/v1/fare/estimate` | Estimate distance, duration, and fare |
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
