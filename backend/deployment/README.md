# EC2 deployment

Fourteen Java services run under systemd on Ubuntu. PostgreSQL, Redis and Kafka run in Docker. Nginx terminates TLS and exposes the gateway. Hosted domain: `swift-app.duckdns.org`; current public address: `16.4.23.48`.

## Prerequisites

Use your own SSH key stored outside the repository. The host needs Java 21, Docker, Compose v2 with `!override` support (2.24.4+), curl, rsync and passwordless sudo. DNS must point to the host and inbound 80/443 must be reachable. Keep infrastructure/service ports private. Bootstrap installs Nginx and Certbot when needed; it does not provision EC2, DNS, security groups, Java or Docker.

## Private configuration

Before first deployment, create `deployment/runtime/service.env` on the host from `deployment/service.env.example`. Directory mode: 700; file mode: 600. Set `DB_USERNAME`, `DB_PASSWORD` and a random base64-encoded 32-byte `JWT_SECRET_KEY`, plus required providers. Compose and systemd use this same private environment.

For an existing installation, preserve its database, JWT and provider settings. Runtime files and volumes are not overwritten by source synchronization. A private backend `.env` can initialize a previously absent runtime environment when bootstrapping directly on the host.

For push, store the Admin JSON in protected runtime storage and set `FIREBASE_CREDENTIALS=file:/absolute/path/to/account.json`. Keep push disabled until configured. No private JSON belongs in static resources. SMTP and Razorpay credentials also stay private.

## Deploy

```bash
cd backend
export SWIFT_DEPLOY_SSH_KEY=/absolute/path/to/your-key.pem
export SWIFT_DEPLOY_HOST=16.4.23.48
export SWIFT_DEPLOY_USER=ubuntu
export SWIFT_DEPLOY_REMOTE_DIR=/home/ubuntu/swiftapp_backend
bash deploy.sh push all
```

The public checkout contains no PEM; supply your own path. Initial deployment requires all services. Later, use `bash deploy.sh push paymentservice` for one service. To deploy directly on the configured host:

```bash
cd /home/ubuntu/swiftapp_backend
bash deployment/bootstrap.sh
bash deploy.sh all
```

Source sync preserves runtime data. All builds finish before activation; a deployment lock prevents overlap. A database backup is taken before infrastructure/schema changes. Services run as the deployment user with protected runtime files; driver documents live under `/var/lib/swiftapp/driver-documents`.

## HTTPS and WebSockets

Bootstrap serves ACME HTTP challenges, obtains a Let's Encrypt certificate and redirects normal HTTP to HTTPS. Set `SWIFT_TLS_EMAIL` for certificate notices. Renewal validates and reloads Nginx.

Nginx proxies unchanged paths to `127.0.0.1:8080`, replaces incoming forwarding headers, supports HTTP/1.1 upgrades for `/ws-native` and `/ws`, disables socket buffering, allows 1-hour socket timeouts and accepts 16 MB uploads. Infrastructure ports bind only to loopback. PostgreSQL initialization adds missing service databases without deleting existing data.

```bash
bash deploy.sh status
curl --fail https://swift-app.duckdns.org/actuator/health
sudo journalctl -u swiftapp@apigateway -n 100
```

Swagger: <https://swift-app.duckdns.org/swagger-ui.html>. Native STOMP: `wss://swift-app.duckdns.org/ws-native`. Verify authenticated API and STOMP behavior with disposable accounts, then test the Flutter app on a device. Do not share logs containing tokens/provider data.

Failed readiness restores and checks the previous JAR release. Database/schema changes are additive and are not rolled back with JARs. Failed first deployment stops new JVMs while preserving data/backups. `bash deploy.sh stop` stops JVM services without deleting infrastructure or volumes.
