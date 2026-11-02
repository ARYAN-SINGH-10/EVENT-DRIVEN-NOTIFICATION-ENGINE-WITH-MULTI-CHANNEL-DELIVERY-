# Event-Driven Notification Engine — Production Hardening & Deployment Guide

This document specifies the operational, infrastructure, security, and deployment guidelines for running the **Event-Driven Notification Engine** in production environments.

---

## 1. Architecture & Dual-Profile Strategy

The application is structured to support seamless zero-dependency local development while enforcing strict resilience, persistent storage, and enterprise compliance in production:

| Capability | Local Development (`default`) | Production Profile (`prod`) |
| :--- | :--- | :--- |
| **Persistence** | Embedded H2 in-memory DB (`jdbc:h2:mem:notificationdb`) | PostgreSQL 15+ with HikariCP connection pooling |
| **Schema Management** | Hibernate `ddl-auto=update`, Flyway disabled | Flyway `V1__initial_schema.sql` versioned migrations, Hibernate `validate` |
| **Event Streaming** | In-Memory Simulation Queue (`app.kafka.simulation-mode=true`) | Real Apache Kafka Cluster with `DeadLetterPublishingRecoverer` |
| **Security** | Permissive localhost CORS, Optional API Keys | Strict Origin Whitelist, Mandatory API Key / Bearer Token |
| **Tracing** | Console logging | Correlation ID in SLF4J MDC, `X-Correlation-ID` HTTP Headers, PII Masking |

---

## 2. Environment Variables & Production Configuration

Activate the production profile by setting:
```bash
SPRING_PROFILES_ACTIVE=prod
```

### Required & Configurable Environment Variables

| Variable | Description | Default (Prod) | Recommended Production Setting |
| :--- | :--- | :--- | :--- |
| `PORT` | HTTP Server Port | `8080` | `8080` (or container default) |
| `DB_HOST` | PostgreSQL Database Host | `localhost` | Managed RDS/Aurora endpoint |
| `DB_PORT` | PostgreSQL Port | `5432` | `5432` |
| `DB_NAME` | Database Name | `notification_engine` | `notification_engine` |
| `DB_USERNAME` | PostgreSQL User | `postgres` | App-specific non-root user |
| `DB_PASSWORD` | PostgreSQL Password | *(empty)* | Secure vault / Secret manager |
| `DB_SSL_MODE` | Database SSL Mode | `prefer` | `require` or `verify-full` |
| `DB_POOL_MAX` | HikariCP Max Pool Size | `25` | Sized to 2–4× CPU cores (25–50) |
| `DB_POOL_MIN_IDLE` | HikariCP Minimum Idle Connections | `10` | `10` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka Brokers | `localhost:9092` | `broker1:9092,broker2:9092` |
| `KAFKA_CONSUMER_GROUP` | Kafka Consumer Group ID | `notification-engine-group` | `notification-engine-group` |
| `KAFKA_SIMULATION_MODE` | Use internal queue instead of broker | `false` | `false` (Real Kafka in Prod) |
| `APP_SECURITY_API_KEY_ENABLED`| Enforce API key/Bearer token on `/api/v1/**` | `false` | `true` |
| `APP_SECURITY_API_KEY` | Secret Master API Key | `prod-secret-api-key-change-me`| 32+ character cryptographic secret |
| `APP_SECURITY_JWT_SECRET` | Secret HMAC for Bearer tokens | `prod-jwt-secret-key-32-chars-minimum`| 256-bit secret key |
| `APP_CORS_ALLOWED_ORIGINS` | Permitted browser origins | `http://localhost:5173,...` | Domain of production frontend |

---

## 3. Database Migration & Schema Management

### Flyway Schema Control
Production relies exclusively on Flyway database migrations located at:
`src/main/resources/db/migration/V1__initial_schema.sql`

The initial migration establishes all 15 relational tables, foreign keys, primary keys, and composite indexes:
1. `notifications` — Core notification records, status lifecycle, retry counters, and foreign keys.
2. `dead_letter_queue` — Dead letter queue entries, error stacks, retry schedules, and resolution audit.
3. `notification_events` — Domain ingestion audit log, idempotency correlation tracking, and payloads.
4. `notification_templates` & `template_versions` — Content templates, parameters, and version rollback tree.
5. `user_preferences` & `user_profiles` — Multi-channel opt-in/opt-out status, quiet hours, and timezones.
6. `compliance_records` — Regulatory compliance records (TRAI, GDPR, TCPA).
7. `dnd_registry` & `dnd_audit_logs` — Do-Not-Disturb scrubbing table and consent validation audit trail.
8. `notification_analytics` — Hourly aggregation metrics (sent, delivered, failed, CTR, latency).
9. `notification_channels` & `delivery_providers` — Channel priority matrices and circuit breakers.
10. `notification_state_audits` — Immutable state transitions with timestamp and trigger reasons.
11. `scheduled_notifications` — Cron and delayed dispatch schedules.

### Migration Execution Procedure
1. Verify database connectivity:
   ```bash
   psql -h $DB_HOST -U $DB_USERNAME -d $DB_NAME -c "SELECT 1;"
   ```
2. When the Spring Boot application starts with `SPRING_PROFILES_ACTIVE=prod`, Flyway automatically applies pending migrations before Hibernate initializes (`spring.jpa.hibernate.ddl-auto=validate`).
3. For dry-run or standalone migration during CI/CD:
   ```bash
   mvn flyway:migrate -Dflyway.url=jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME -Dflyway.user=$DB_USERNAME -Dflyway.password=$DB_PASSWORD
   ```

### Rollback Strategy
If a migration fails or must be rolled back:
1. Migrations are idempotent and transactional where supported by PostgreSQL DDL.
2. To roll back a schema migration, apply a compensatory migration script named sequentially (e.g., `V2__revert_feature.sql`).
3. Repair the Flyway schema history if a migration had a transient error:
   ```bash
   mvn flyway:repair -Dflyway.url=...
   ```

---

## 4. Apache Kafka Cluster Setup & Resilience

### Topic Definitions
The engine provisions or expects the following Kafka topics with minimum partition and replication settings:

| Topic Name | Minimum Partitions | Replication Factor | Retention | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| `notification-events` | 6 | 3 | 7 days | Ingestion domain events |
| `notification-delivery` | 6 | 3 | 3 days | Channel dispatch and worker execution |
| `notification-dlq` | 3 | 3 | 30 days | Poison-pill messages & exhausted retries |

### Consumer Groups & Dead-Letter Routing
- **Consumer Group**: `notification-engine-group`
- **Offset Commit**: Manual acknowledgement (`AckMode.RECORD`) after successful dispatch.
- **Error Handling**: `DefaultErrorHandler` configured with a `DeadLetterPublishingRecoverer`.
- **Backoff & Retries**: 3 retries with 1000ms fixed backoff.
- **Dead-Letter Destination**: Exhausted messages are automatically routed to `notification-dlq` with Kafka headers containing exception message, stack trace, and original topic/partition.

---

## 5. Reliable Asynchronous Processing & Idempotency

### Event Ingestion Lifecycle (HTTP 202)
1. **HTTP Ingestion**: Client submits domain event via `POST /api/v1/events`.
2. **Idempotency Guard**:
   - The engine checks `NotificationEventRepository.findByCorrelationId(correlationId)`.
   - If an event with the same `correlationId` exists, the engine logs an idempotency match and immediately returns the existing event reference without double-publishing.
3. **Audit Persistence**: New events are persisted to `notification_events` with `processed = false`.
4. **Asynchronous Dispatch**: The event is pushed to Kafka topic `notification-events` (or simulation queue in local dev) and returns `HTTP 202 ACCEPTED` with the assigned UUID.
5. **Worker Execution**: Worker consumer routes to appropriate channel processors (In-App, Push, Email, SMS, WhatsApp), checks DND compliance, enforces frequency capping, and dispatches to delivery providers.
6. **Application Restart Recovery**:
   - On application startup, `@EventListener(ApplicationReadyEvent.class)` invokes `NotificationEventService.recoverUnprocessedEvents()`.
   - Any stale in-flight events (`processed = false` created > 2 minutes prior) are automatically re-queued for processing.

---

## 6. Security & Sensitive Data Protection

### API Authentication & Authorization
- **Header Authentication**: Supported via `X-API-Key: <secret>` or `Authorization: Bearer <token>`.
- **Whitelisted Endpoints**: `/actuator/health`, `/api/swagger-ui/**`, `/api/v3/api-docs/**`, `/index.html`, and static UI assets.
- **Controlled Endpoints**: All `/api/v1/**` endpoints require valid credentials when enabled via `app.security.api-key-enabled=true`.

### Production CORS Rules
CORS is restricted in production via `app.cors.allowed-origins`. Only explicit trusted frontend origins are allowed with credentials enabled.

### PII Masking in Logs
All logs protect sensitive customer data:
- **Email addresses**: `john.doe@enterprise.com` $\rightarrow$ `j***@enterprise.com`
- **Phone numbers**: `+12025550199` $\rightarrow$ `+120***`
- **Device tokens**: Truncated to first 8 characters.

---

## 7. Observability & Health Indicators

### End-to-End Distributed Tracing
- **Correlation ID Filter**: Every HTTP request receives an `X-Correlation-ID` header (reused if provided by caller or generated as UUID).
- **MDC Propagation**: The correlation ID is placed into SLF4J MDC (`correlationId`), ensuring all log statements generated across downstream components include the correlation ID.
- **Log Format**: `TIMESTAMP [THREAD] [MDC_CORRELATION_ID] LEVEL LOGGER - MESSAGE`

### Actuator Health Endpoint
Access health metrics at `GET /actuator/health`:
```json
{
  "status": "UP",
  "details": {
    "notificationEngine": {
      "status": "UP",
      "details": {
        "environmentMode": "SIMULATION",
        "activeProviders": 5,
        "healthyProviders": 5,
        "pendingDlqCount": 0
      }
    }
  }
}
```
If more than 50% of external delivery providers fail or the DLQ pending backlog exceeds 1,000 items, the component health degrades to `DOWN` / `OUT_OF_SERVICE`.

---

## 8. Deployment Verification Checklist

Prior to promoting a release to production:

- [x] All automated tests pass: `mvn test` (65 tests run, 0 failures, 0 errors).
- [x] Static frontend assets compiled and embedded: `npm run build` completed cleanly.
- [x] PostgreSQL target database created with `uuid-ossp` or `gen_random_uuid()` support.
- [x] Flyway migrations validated against target database: `mvn flyway:info`.
- [x] Kafka topics (`notification-events`, `notification-delivery`, `notification-dlq`) provisioned.
- [x] Environment secrets (`DB_PASSWORD`, `APP_SECURITY_API_KEY`) populated from Vault / Secret Manager.
- [x] Health check monitoring configured on `GET /actuator/health`.
- [x] Log aggregation (e.g., Datadog, ELK) configured to index the `correlationId` field.
