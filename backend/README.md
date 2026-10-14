# Swift backend

OTP verification retains the code until the authenticated app acknowledges a loaded home screen, or the existing 60-second expiry. See [the auth handoff contract](authservice/README.md) for request/response, retries and upgrade details.

[Watch the rider and driver demo](https://github.com/freaknu/swift_app/issues/1) · [Live API docs](https://swift-app.duckdns.org/swagger-ui.html) · [Architecture and event flows](../docs/architecture/README.md)

[![SwiftApp demo](../docs/demo/swift-app-demo-poster.png)](https://github.com/freaknu/swift_app/issues/1)

Java 21 / Spring Boot services for Swift. Spring Cloud Gateway provides public routing, JWT checks, rate limiting and Swagger. Eureka discovers services, PostgreSQL stores data, Kafka carries domain events, Redis supports matching and real-time delivery, and STOMP delivers ride, payment, location and chat updates.

## Services

| Module | Port | Responsibility |
| --- | --- | --- |
| `eurekaserver` | 8761 | Discovery |
| `apigateway` | 8080 | Public API and Swagger |
| `authservice` | 8081 | Authentication |
| `userservice` | 8082 | User profiles |
| `driverservice` | 8096 | Drivers, vehicles, documents, availability |
| `riderservice` | 8083 | Ride lifecycle and durable events |
| `locationservice` | 8087 | Location and ETA |
| `matchingservice` | 9005 | Matching and offers |
| `paymentservice` | 9910 | Razorpay workflow and payment events |
| `websocketservice` | 8085 | Native STOMP and SockJS |
| `chatservice` | 9004 | Durable chat |
| `noticationservice` | 8088 | Email and optional push; spelling retained |
| `ratingservice` | 8086 | Ratings |
| `analyticalservice` | 8089 | Analytics |

`aiservice` is an unconfigured scaffold and is excluded from deployment.

## Local development

Install Java 21, Docker/Compose v2, Bash, Python 3, OpenSSL and netcat (`nc`). Each module includes a Maven wrapper.

```bash
cd backend
bash scripts/configure-local.sh
bash start-infrastructure.sh
bash build-services.sh --skip-tests
bash start-services.sh
```

The initializer generates fresh database and JWT secrets in ignored `.env` with mode 600, preserving an existing file. Compose reads `.env`; the service launcher exports it to JVMs. All authentication, gateway and WebSocket services must share `JWT_SECRET_KEY`. No real credentials are committed to YAML/properties.

Open <http://localhost:8080/swagger-ui.html>. Local email is captured at <http://localhost:8025> with `SWIFTAPP_CAPTURE_EMAILS=1`. External SMTP is optional. Maps, payments and push need your own provider settings.

When reusing an existing database volume, retain its username/password in the private `.env`. Changing Compose values does not change an existing database account's password. The initialization script adds missing service databases without deleting data.

### IntelliJ run configurations

Import the Maven modules with Java 21. Put your private `.env` values into each run configuration's environment, especially `JWT_SECRET_KEY`, `DB_USERNAME` and `DB_PASSWORD`. IntelliJ does not automatically export a shell `.env`. Start infrastructure, Eureka, application services and then the gateway. Keep any existing private local properties/YAML configuration unchanged; do not replace it with another environment's credentials.

`bash status.sh` reports service state; `bash stop-services.sh` stops JVMs while preserving Docker infrastructure and database volumes.

## Provider configuration

| Variables | Purpose |
| --- | --- |
| `JWT_SECRET_KEY` | Same random base64-encoded 32-byte signing key for auth/gateway/WebSocket |
| `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL account |
| `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET` | Your Razorpay test/live credentials |
| `RAZORPAY_WEBHOOK_SECRET` | Optional until you configure a signed provider webhook |
| `GOOGLE_MAPS_API_KEY` | Backend Maps/Routes key |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP; optional with Mailpit |
| `PUSH_ENABLED`, `FIREBASE_CREDENTIALS` | Enable push with an external `file:/absolute/path/account.json`, or Application Default Credentials |

Use `.env.example` locally and `deployment/service.env.example` for EC2. Provider secrets stay on the backend. Never put JWT signing, Razorpay secret or Firebase Admin credentials in Flutter.

## Payments and events

Ride completion persists an event in an outbox. Kafka delivers it to payment processing; payment status events are persisted and forwarded through WebSockets to authorized trip participants. Checkout is verified by the backend before success. Unknown provider outcomes remain pending for reconciliation.

Razorpay calls run outside database transactions/payment locks. Outbox retries are scheduled per event with quarantine, so a failed event does not stop unrelated work. See [payment documentation](paymentservice/README.md) and [ride documentation](riderservice/README.md).

## Swagger, sockets and deployment

Hosted Swagger: <https://swift-app.duckdns.org/swagger-ui.html>. It aggregates 11 service schemas and uses the public API origin. Select **Authorize** and provide an access token for protected operations.

Flutter uses `wss://swift-app.duckdns.org/ws-native` and sends its access token in STOMP CONNECT. HTTP upgrade does not authenticate subscriptions. SockJS uses `/ws`; HTTP fallbacks and socket upgrades are routed separately.

```bash
bash build-services.sh
bash deployment/test-deploy.sh
bash scripts/check-architecture.sh
```

Some integration tests require PostgreSQL/Kafka or explicit environment settings. Architecture checks may report existing layering exceptions; inspect output before claiming a clean result. See [EC2 deployment](deployment/README.md) for HTTPS/WSS, private configuration and rollback. Initial deployment includes all 14 runnable modules; later changes can target one.
