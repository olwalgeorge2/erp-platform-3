API Gateway Health Routing Runbook

- Symptoms
  - 401 on `GET /api/v1/identity/health/*` via gateway while direct `GET :8081/q/health/*` is 200.
  - ConfigValidationException: `gateway.routes[0].rewrite.add-prefix` empty String considered null.
  - Testcontainers error: Redpanda container networking "address already in use" during gateway dev.
  - Kafka throttling warning in identity: `TooManyMessagesWithoutAckException`.

- Root Causes
  - Missing or empty `rewrite.add-prefix` in `application.yml`.
  - Public paths not including versioned health aliases.
  - Kafka DevServices auto-starting Redpanda conflicts with existing Docker ports.
  - Reactive messaging consumer not acking; backlog triggers throttling.

- Fix Checklist
  - Gateway config `api-gateway/src/main/resources/application.yml`:
    - Identity platform routes (non-overlapping):
      - `/api/v1/identity/q/*` → rewrite remove-prefix `/api/v1/identity`, add-prefix `/`.
      - `/api/v1/identity/api/*` → rewrite remove-prefix `/api/v1/identity`, add-prefix `/`.
    - Versioned health aliases:
      - `/api/v1/identity/health` and `/api/v1/identity/health/*` → rewrite to `/q/health`.
    - Public endpoints:
      - Include `/api/v1/identity/q` and `/api/v1/identity/health` under `gateway.public-prefixes`.
  - Public endpoint matcher `PublicEndpointsConfig.kt`:
    - Normalizes both `path` and `path/` so trailing slashes don’t cause false 401.
  - Disable Kafka DevServices in dev for gateway:
    - In `%dev` profile: `quarkus.kafka.devservices.enabled=false`.
  - Identity service (dev):
    - If not consuming events, disable incoming channel: `mp.messaging.incoming.identity-events-in.enabled=false`.
    - Ensure DB points to running Postgres (Docker): `jdbc:postgresql://localhost:5432/erp_identity`.

- How to Restart Cleanly
  - Stop running dev instances to clear stale reload state.
  - Start identity (port 8081): `./gradlew :bounded-contexts:tenancy-identity:identity-infrastructure:quarkusDev`.
  - Start gateway (port 8080): `./gradlew :api-gateway:quarkusDev`.
  - Verify health alias routing:
    - `GET http://localhost:8080/api/v1/identity/health` → 200.
    - `GET http://localhost:8080/api/v1/identity/health/ready` → 200.
    - `GET http://localhost:8080/api/v1/identity/q/health/ready` → 200.
    - Direct identity: `GET http://localhost:8081/q/health/ready` → 200.

- Common Errors & Resolutions
  - Duplicate YAML key warning `duplicate keys found : mp` during live reload:
    - Benign if profiles split across `application.yml` and `application-dev.yml`. If in a single file, ensure `mp:` appears once per document.
  - `add-prefix is empty` failure:
    - Ensure `rewrite.add-prefix` is set to `/` not `""`.
  - Redpanda container 500 "address already in use":
    - Disable DevServices in the module or stop existing Redpanda/ Kafka containers.
  - 401 on health alias:
    - Confirm `gateway.public-prefixes` includes `/api/v1/identity/health` and `/api/v1/identity/q`.
    - Restart gateway dev; live reload might not rewire filters.

- Quick Test Snippets (PowerShell)
  - `(Invoke-WebRequest -UseBasicParsing http://localhost:8080/api/v1/identity/health/ready).StatusCode`
  - `(Invoke-WebRequest -UseBasicParsing http://localhost:8081/q/health/ready).StatusCode`

