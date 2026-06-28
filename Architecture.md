# System Architecture

The Event-Driven Notification Engine is designed as an asynchronous, highly decoupled, and resilient microservice.

## High Level Architecture

```text
 [Upstream Services] -> (REST/Kafka) -> [Ingestion API]
                                               |
                                        [Event Store DB]
                                               |
                                     (Spring App Events / Kafka)
                                               |
                                      [Processing Pipeline]
                                     /          |          \
                        [Compliance]   [Routing]   [Personalization]
                               |            |            |
                               +------------+------------+
                                            |
                                  [Channel Processors] (SMS, Email, Push)
                                            |
                                     [Provider APIs]
```

## Core Pipelines

### 1. Event Flow
1. **Ingestion**: Raw events are ingested via REST (`/api/v1/events`) or Kafka topics.
2. **Persistence**: The event is immediately persisted to the `NotificationEvent` table to ensure no data loss (Idempotency check).
3. **Dispatch**: The event is published asynchronously via `DomainNotificationEvent`.

### 2. Kafka Flow (Optional)
If `app.kafka.simulation-mode` is `false`, ingested events are published to a Kafka topic. A `@KafkaListener` consumes these messages, allowing the engine to be horizontally scaled with consumer groups processing the load.

### 3. Notification Lifecycle & State Machine
Every notification relies on a strict State Machine (`NotificationStateMachineService`) which logs transitions to the `NotificationAudit` table:
`CREATED` -> `ENRICHED` -> `ROUTED` -> `QUEUED` / `QUIET` / `DND` / `CAPPED` -> `SENT` -> `DELIVERED` -> `READ`
*Failures lead to `RETRYING`, and eventually `DLQ`.*

### 4. Channel Routing
`ChannelRoutingService` intercepts the payload. If explicit channels are missing, it uses a multi-tier logic:
1. Filters out channels the user opted out of.
2. Selects the most cost-effective channel based on priority.
3. Ensures deliverability fallback (e.g. if Push fails, fallback to SMS).

### 5. Compliance Flow
1. **DND (Do Not Disturb)**: The `DndComplianceEngine` matches the `FinancialEventType`. If a promotional event matches a user registered on a national DND registry, it drops state to `DND`.
2. **Frequency Capping**: `FrequencyCapService` prevents spam by counting sent notifications in Redis/DB caches. Drops state to `CAPPED` if thresholds are exceeded.
3. **Quiet Hours**: `QuietHoursService` analyzes the user's localized time. If within sleep hours, the event is scheduled to an internal queue and drops state to `QUIET`.

### 6. DLQ Flow
When a provider throws a timeout or 500 error, the engine intercepts it.
1. Checks `retryCount < maxRetries`.
2. If true, state goes to `RETRYING`. An exponential backoff calculation schedules a future execution.
3. If false, state goes to `DLQ`. Operations can view, delete, or replay it via the `DlqController`.

### 7. Analytics Pipeline
The `AnalyticsAggregator` fires asynchronously on every state change.
A background `@Scheduled` job (`AnalyticsRollupScheduler`) calculates totals (Delivery Rate, CTR, Time-to-Delivery) and commits daily snapshots to `NotificationAnalytics`.
