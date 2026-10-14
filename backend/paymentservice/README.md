# Ride payment service

The service consumes `ride.completed`, persists one payment and one Razorpay order per ride, and sends payment status events through a database outbox. The rider opens Razorpay Checkout using the server-created order. A signed callback or webhook is verified and the payment is fetched from Razorpay; `COMPLETED` is emitted only after the funds are captured.

## Run

Use Java 21, PostgreSQL, Kafka, Eureka, rider service and user service. Create `paymentservice_db` before starting. Configuration and credentials can stay in `src/main/resources/application.properties` or your application YAML. The existing placeholders allow these optional environment overrides:

| Variable | Purpose / default |
| --- | --- |
| `RAZORPAY_KEY_ID` | Razorpay test or live public key ID |
| `RAZORPAY_KEY_SECRET` | Matching server API secret |
| `RAZORPAY_WEBHOOK_SECRET` | Optional for local Checkout testing; required only if using signed Razorpay webhooks |
| `PAYMENT_DB_URL` | `jdbc:postgresql://localhost:5432/paymentservice_db` |
| `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL credentials configured in the application properties |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| `EUREKA_URL` | `http://localhost:8761/eureka/` |
| `RIDER_SERVICE_URL` | `http://localhost:8083` |
| `USER_SERVICE_URL` | `http://localhost:8082` |
| `PAYMENT_SERVICE_PORT` | `9910` |
| `SERVICE_BIND_ADDRESS` | `127.0.0.1`; only expose this service through the authenticated API gateway |
| `SERVICE_ADVERTISE_ADDRESS` | `127.0.0.1`; Eureka address used by the gateway to reach this service |
| `PAYMENT_CONSUMER_ENABLED` | `true` |
| `PAYMENT_OUTBOX_ENABLED` | `true` |
| `PAYMENT_OUTBOX_INTERVAL_MS` | `1000` |

For IntelliJ, run the existing **Payment Service** configuration (`PaymentserviceApplication`, module `paymentservice`, Java 21). Its build step copies the application properties into the runtime classpath; no environment-variable migration is required. Start PostgreSQL, Kafka, Eureka, rider service and user service, then run Payment Service and API Gateway. Restart Payment Service after changing its configuration so Eureka receives its updated address.

For local IntelliJ runs, payment service binds to `127.0.0.1` and advertises that same address in Eureka. If these addresses differ, gateway payment requests can fail even while the service is running. Set both overrides appropriately when services run on different hosts.

```sh
./mvnw spring-boot:run
```

The application starts without configured Razorpay keys, but order creation/verification returns a configuration error until keys are supplied. The service trusts `x-user-id` from the gateway, forwards it to the participant-checked rider endpoint, and resolves the authenticated rider profile through user service for order creation and verification. Do not publish the service port directly.

For your local IntelliJ test-mode setup, the API key ID and API key secret are enough for the Checkout flow. Leave your existing `razorpay.webhook-secret` value empty if you have not configured a webhook. The app submits Checkout's order ID, payment ID and signature to `/api/v1/payments/rides/{rideId}/verify`. The server validates that signature using the **API key secret**, fetches the payment from Razorpay and captures it if authorized, then publishes `payment.completed` for the websocket/UI success flow. The webhook secret does not participate in this path. No existing properties, YAML values or credentials need to be moved or replaced.

Test mode also supports webhooks, but their secret is a separate value you choose when configuring the endpoint; it is not supplied automatically by the API test keys. If you later want webhook recovery when a customer closes the app before verification reaches the server, configure an externally reachable HTTPS gateway webhook URL ending in `/api/v1/payments/webhook`, choose a webhook secret, and subscribe to `payment.authorized`, `payment.captured`, `payment.failed` and `order.paid`. Store the same value in the existing `razorpay.webhook-secret` setting. The webhook endpoint requires the exact raw bytes and `X-Razorpay-Signature`; when no webhook secret is configured, it rejects webhook processing with HTTP 503 instead of accepting unverified events. A Checkout callback alone never marks a ride paid.

These signing rules follow Razorpay's [Checkout signature verification](https://razorpay.com/docs/payments/payment-gateway/web-integration/standard/integration-steps/) and [webhook setup](https://github.com/razorpay/markdown-docs/blob/master/payments/dashboard/account-settings/webhooks.md) documentation. [Test-mode webhooks](https://razorpay.com/docs/webhooks/validate-test/) use the same raw-body signature validation as live-mode webhooks.

## API

All responses use the existing `ApiResponseFormat` shape (`data`, `statusCode`, `message`, `endpoint`, `success`, `errors`, `timestamp`).

| Endpoint | Access / behavior |
| --- | --- |
| `GET /api/v1/payments/rides/{rideId}` | Ride participants; reads persisted status or initializes a pending payment from the completed ride's authoritative fare |
| `POST /api/v1/payments/rides/{rideId}/order` | Ride's authenticated rider; creates or recovers the persisted order |
| `POST /api/v1/payments/rides/{rideId}/verify` | Same rider; body `{razorpayOrderId, razorpayPaymentId, razorpaySignature}` |
| `POST /api/v1/payments/webhook` | Raw signed Razorpay webhook |

Payment `data` is `{rideId,riderId,driverId,status,razorpayKeyId,razorpayOrderId,amount,currency,razorpayPaymentId}`. `amount` is an integer in paise. Status values are `PENDING`, `ORDER_CREATED`, `COMPLETED`, `FAILED`. The client must render payment success only for `COMPLETED`.

Only INR rides are supported. The server rejects missing/unsupported currency, amounts below INR 1.00, and fares with more than two decimal places. Clients never submit a fare or amount to the order endpoint.

## Events and recovery

Input topic `ride.completed` uses `{rideId,riderId,driverId,totalFare,currency,completedAt}`. `totalFare` is a decimal in major units. Additional fields are ignored. Invalid events and transient failures are not acknowledged; Kafka retries them. A permanently invalid record requires operator correction/recovery before that partition can advance.

On a failed Kafka attempt, the service logs the topic, partition, offset, exception types and code locations before retrying. Unexpected HTTP failures log the method, endpoint and the same safe diagnostics while returning a generic error to the client. Request bodies, authorization headers, exception messages and provider response bodies are excluded from these logs. Repeated `Seeking to offset` messages mean that the record is being retried; use the preceding payment failure entry to find its cause.

Output topics are `payment.order.created`, `payment.completed`, `payment.failed`. Their flat payload is `{eventId,rideId,riderId,driverId,paymentStatus,razorpayOrderId,razorpayPaymentId,amount,currency,eventTime}`. `eventTime` is an ISO UTC instant and `amount` is in paise. Secrets, callback signatures and Checkout key IDs are excluded from events. Websocket service delivers these status events to the ride participants.

The ride primary key prevents duplicate initialization without overwriting a concurrent winner. Provider operations use a persisted three-minute lease: short transactions lock the payment only to claim work, validate/apply a result or release the lease. Razorpay order lookup/creation and payment fetch/capture execute outside database transactions, including when the calling code already has a transaction. Another request can read or lock the payment while the provider is slow. Concurrent requests wait at most ten seconds outside transactions, with bounded polling backoff, then return a retryable HTTP 503 if work remains in flight. Fresh provider state is fetched after claiming capture, and every apply phase checks the current persisted state; captured funds cannot regress, and stale failed snapshots cannot undo a newer explicit retry.

The original receipt and a creation-attempt timestamp are committed before the Razorpay order POST. Retries recover that exact receipt using Razorpay's [receipt search](https://razorpay.com/docs/api/orders/fetch-all/), which is a contains filter; the adapter checks exact matches across pages and rejects ambiguous duplicate matches. A definite rejection before creation clears the attempt marker so retry is safe. A timeout, crash or uncertain provider response keeps the marker. If the receipt lookup is empty after such an uncertain attempt, the service returns HTTP 503 and does **not** issue another POST. This protects against duplicate payable orders but leaves that payment pending until the provider result is reconciled. A lost capture reply is recovered by fetching the already-captured payment. Webhooks can recover an order by its stored receipt even before the original POST response is committed.

The additive payment upgrade is `src/main/resources/db/upgrades/20261014_payment_provider_operations.sql`. The current IntelliJ/Hibernate-update setup adds its four nullable columns automatically on restart; production deployments using `ddl-auto=validate` must apply the SQL before boot. New rows explicitly store provider workflow version 1. Existing rows retain a null version, with no database default or backfill: an order-less legacy row may have an accepted POST that the older code never recorded. The upgraded service can recover a found order for that receipt, but an empty lookup cannot authorize another POST. This protection applies automatically after either Hibernate or the manual migration, and repeating the SQL preserves the unknown legacy history.

If a legacy row or uncertain attempt stays pending, reconcile its provider outcome before allowing another create operation. Do not clear an attempt marker or mark a legacy row as version 1 solely because receipt search returns no result. Stop older payment-service instances during rollout so legacy writers cannot bypass this protocol; obtain definitive provider evidence before an operator enables retry on an unresolved row.

Payment state and output events commit together. The dispatcher marks an event published only after Kafka acknowledges it. Delivery is at least once: a crash between broker acknowledgment and the database update can produce duplicate messages, so consumers must tolerate them. Captured payments are terminal; later failure notifications cannot regress them.

A failed Checkout attempt retries the same persisted order. Razorpay's [order state documentation](https://razorpay.com/docs/api/orders/fetch-with-id/) describes multiple attempts and prevents additional payments once an order is paid; the [payment webhook documentation](https://github.com/razorpay/markdown-docs/blob/master/webhooks/payments.md) also describes a failed notification followed by later capture. The current [create-order page](https://razorpay.com/docs/api/orders/create/) contains conflicting advice to create a new order after a failed attempt. This implementation preserves the one-order-per-ride requirement to avoid leaving multiple orders payable. Validate retry behavior with your Razorpay test account before launch; automatic real-provider retry behavior has not been exercised here.

## Verification

```sh
./mvnw test
```

The tests use real H2/JPA transactions, database locks, HTTP controller validation and stubbed external ports. They cover the Kafka event contract, authoritative identity/fare access, forged signatures, amount/currency/order/payment mismatch, authorized capture, pending-state handling, signed raw webhook bytes, duplicate/late events, provider-response crash recovery, concurrent initialization/verification, stable-order retry, outbox publishing retry, and API Checkout verification with an empty webhook secret. Blocked-provider tests verify that order creation, payment fetch and capture run with no active transaction and leave the row available to another transaction. They cover cross-instance claims, caller transaction suspension, paid webhooks during in-flight provider calls, stale failure/retry races, lease expiry, unknown creation outcomes, definite rejection, lost capture replies, legacy-row upgrade safety and repeatable schema migration. Separate offline adapter tests use actual Razorpay SDK order/payment entities to verify exact receipt filtering, pagination, duplicate receipt rejection, definite-versus-ambiguous failures and String/Integer/Long field conversion for create, recover, fetch and capture. Diagnostic tests verify that exceptions are rethrown for retry and sensitive messages/payloads are excluded from logs. No test makes a real charge or requires live keys.

Production PostgreSQL/Kafka and real Razorpay test-mode Checkout/webhook delivery still require an environment smoke test. Hibernate `ddl-auto=update` follows the repository's existing development setup; use managed migrations for production schema changes.

## Durable outbox retry and quarantine

The publisher commits a short database lease before sending to Kafka, then commits
acknowledgement or failure in a separate short transaction. It does not hold a
payment or outbox row lock during broker I/O. Up to 16 distinct ride heads can be
in flight per instance; an unacknowledged/failed ride cannot fill a batch with its
successors. An event is marked published only after broker acknowledgement.

Retry attempts, next-attempt time and lease ownership are persisted. Retry waits
are 1, 2, 4, 8, 16, 32 and 60 seconds, with at most 8 sends per replay. A 30-second
lease allows another instance to recover after a crash; Kafka acknowledgement is
bounded at 10 seconds. A stale callback cannot overwrite a newer lease owner.
Serialization, invalid-topic, oversized-record and authorization failures go
directly to quarantine; exhausted transient retries are also quarantined. Only
exception type is retained, never exception text or payment payload in diagnostics.

The database identity `delivery_sequence` orders payment events for each ride,
including transitions with identical timestamps and reverse-sorted UUIDs. A
quarantined head deliberately blocks its own later transitions until corrected
and replayed, while unrelated rides continue. Delivery remains at least once, so
downstream consumers must deduplicate event IDs and treat captured payments as
terminal. Identity values backfill existing queued rows when the additive column
is introduced; the original causal order of historical equal-time UUID events
cannot be recovered from the old schema. Inspect historical ambiguous rows before
replay if any are present.

The additive, idempotent PostgreSQL upgrade is
`src/main/resources/db/upgrades/20261014_outbox_delivery.sql`. The startup upgrade
runs automatically in the current Hibernate-update development setup. Apply the
same SQL before starting a production instance that uses `ddl-auto=validate`.
Existing rows and payloads are preserved. Properties, YAML and credentials are
unchanged.

Operators can inspect `payment_event_outbox` rows with `quarantined_at IS NOT NULL`
(or `PaymentOutboxJpaRepository.findByQuarantinedAtIsNotNullOrderByQuarantinedAtAsc`).
After correcting the broker/topic/permission or payload cause, invoke the
operator-only bean method `PaymentOutboxDispatcher.replayQuarantined(eventId)`.
There is deliberately no unauthenticated/public replay endpoint. For an offline
operator SQL workflow, stop the relevant dispatcher instance(s) first, inspect
the row, correct the cause, then replay one reviewed ID:

```sql
UPDATE payment_event_outbox
SET quarantined_at = NULL, delivery_attempts = 0,
    next_attempt_at = CURRENT_TIMESTAMP, delivery_token = NULL,
    lease_until = NULL, last_failure_type = NULL
WHERE event_id = 'REVIEWED_EVENT_ID'
  AND quarantined_at IS NOT NULL AND published_at IS NULL;
```

Do not bulk reset pending/leased events: replay may cause duplicate Kafka delivery.
