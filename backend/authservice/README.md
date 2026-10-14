# Authentication OTP handoff

The existing authentication service, database, Redis adapter and JWT architecture are unchanged.

1. `GET /api/v1/auth/sendOtp/{email}` stores a six-digit OTP with its existing **60-second expiry**. A resend atomically replaces the code and removes the previous verification/session markers.
2. `GET /api/v1/auth/verifyOtp/{email}/{otp}` checks the code without deleting it or extending its expiry. A valid response has HTTP 200, `success: true` and boolean `data: true`; incorrect, expired or already claimed codes return HTTP 400 with both values false. Incorrect attempts do not erase the code.
3. Login requires the verified code in its existing `otp` field. Signup requires the server-side verification performed immediately before signup. Successful login/signup claims the OTP once, returns the normal credentials plus an opaque `otpCompletionToken`, and leaves the original code in Redis. A claim cannot issue another login session.
4. Once the authenticated rider or driver home screen has loaded and displayed its required data, the client calls `POST /api/v1/auth/home-ready` with `Authorization: Bearer <accessToken>` and JSON `{ "otpCompletionToken": "<returned value>" }`.
5. The service validates the signed access token and its persisted, active account/session. Redis clears the code and its markers only when the acknowledgement matches both the unique completion token and access session. Missing, expired, forged, refresh-only or logged-out sessions cannot clear an OTP. An acknowledgement for an older session cannot delete a resent code, even if the numeric OTP happens to be the same.

The client persists the completion token with its credentials and retries the acknowledgement after a transient network failure. The acknowledgement is idempotent: an already cleared or expired claim, or a claim replaced by a resend, returns success without deleting another code. Home-screen navigation does not wait for this cleanup request.

The original 60-second expiry remains the upper bound for the OTP and its Redis markers, including when the home screen cannot load. A failed server-side login transaction releases only its matching claim so the same unexpired code can be retried. If the login response is lost or the client cannot save its returned credentials, the user must **request a new OTP**; retaining the old code does not make it reusable for issuing another session.

## Existing database upgrade

Access and refresh JWTs are stored as PostgreSQL `TEXT`. A signed token can exceed 255 characters, particularly with a longer email and the unique session `jti` claim; truncating it would invalidate its signature. Before deploying this update to an existing authentication database, apply `deployment/migrations/20261015_expand_session_tokens.sql` using the deployment's existing secured PostgreSQL connection. The narrow migration widens both `user_credentials` token columns, preserves existing values and can be reapplied safely. No migration framework or database connection configuration is added.

`UserCredentialsTokenPersistenceTest` generates real signed access/refresh tokens longer than 255 characters, checks the generated JPA schema's capacity, then saves, reloads and queries the complete token values in the test database.

Run the auth suite using Java 21:

```sh
./mvnw test
```

`RedisOtpLifecycleTest` starts an isolated ephemeral `redis-server` process and never accesses the configured application Redis database. Install `redis-server` or set `REDIS_SERVER_BINARY` to its executable path to run those six integration tests; they are skipped explicitly when the binary is unavailable.
