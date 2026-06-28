# Event-Driven Notification Engine

An enterprise-grade, highly scalable, asynchronous microservice designed to ingest domain events and route them to users through optimal communication channels. Built for high resiliency, the engine features strict compliance controls, frequency capping, quiet hour queueing, and a robust Dead Letter Queue (DLQ).

## Features
- **Multi-Channel Delivery:** Pluggable processors for SMS, Email, Push, WhatsApp, Slack, In-App, Webhooks, and Voice.
- **Intelligent Routing:** Cost-optimized and fallback-aware channel selection.
- **Resiliency & Retries:** Exponential backoff and maximum retry tracking with DLQ routing for poison messages.
- **Compliance Engine:** Strict TRAI DND (Do Not Disturb) promotional checks based on event taxonomy.
- **Frequency Capping:** Velocity limits on a global, channel, or taxonomy level (critical events bypass limits).
- **Quiet Hours:** Timezone-aware queueing to prevent notifications during user sleep windows.
- **Deep Analytics & Dashboard:** Asynchronous metric aggregation (Delivery Rate, CTR, Cost-Per-Notification).
- **Full Traceability:** Robust State Machine mapping every payload transition to an immutable Audit Log.

## Financial Event Taxonomy
All ingested events are strictly mapped to the `FinancialEventType` taxonomy to enforce compliance:
- **TXNX-001 (Transactional)**: E.g., Payment success, OTP. (Bypasses DND).
- **PROMO-001 (Promotional)**: E.g., Loan offers. (Blocked by DND, respects Quiet Hours).
- **CRIT-001 (Critical/Fraud)**: E.g., Account takeover. (Bypasses all caps, quiet hours, and DND).

## Tech Stack
- **Framework:** Java 21 / Spring Boot 3.3.5
- **Persistence:** Spring Data JPA / Hibernate (PostgreSQL recommended)
- **Messaging:** Apache Kafka (configurable internal simulation mode)
- **Documentation:** Springdoc OpenAPI / Swagger UI
- **Testing:** JUnit 5, Mockito, Maven Surefire

## Project Structure
```text
src/main/java/com/notificationengine/
 ├── analytics/    # Aggregation and metric rollup schedulers
 ├── channel/      # Interfaces and channel-specific processors
 ├── controller/   # REST APIs (Events, Preferences, DLQ, Analytics, Dashboard)
 ├── dlq/          # Dead Letter Queue handling
 ├── dto/          # Data Transfer Objects
 ├── entity/       # JPA Entities
 ├── event/        # Ingestion pipeline, Kafka producers/listeners
 ├── preference/   # User preference resolution
 ├── repository/   # JPA Repositories
 ├── service/      # Core business logic (State Machine, Routing, Compliance)
 ├── taxonomy/     # Financial Event enums
 └── template/     # Template rendering engine
```

## Running Locally

### Prerequisites
- JDK 21
- Maven 3.9+
- *(Optional)* Kafka and Zookeeper (for non-simulated mode)

### Configuration
By default, the application runs on an H2 in-memory database and uses a Simulated Event Bus instead of Kafka.
You can configure a persistent Database (PostgreSQL) and Kafka in `src/main/resources/application.properties`:
```properties
app.kafka.simulation-mode=true # Set to false to use real Kafka topics
spring.datasource.url=jdbc:postgresql://localhost:5432/notification_db
```

### Build & Run
```bash
mvn clean install
mvn spring-boot:run
```

## API Documentation & Swagger URL
When the application is running, full interactive OpenAPI documentation is available at:
👉 **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

*(Detailed offline documentation is available in `API_Documentation.md`)*

## Testing & Build Results
The project contains 60 comprehensive unit and integration tests covering standard pathways and edge-case failure scenarios.
```bash
mvn clean test
mvn surefire-report:report
```
*Current Status: 100% BUILD SUCCESS. View the generated report at `target/site/surefire-report.html`.*

## Architecture Reference
See the `Architecture.md` file for deep-dives into the pipeline, State Machine, Kafka flows, and Data schemas (`DatabaseSchema.md`).

## Future Enhancements
- **Redis Integration:** Offload Frequency Capping state and Template Caching to Redis for multi-instance scaling.
- **GraphQL API:** Implement a GraphQL layer for the Dashboard for selective metric fetching.
- **Provider Webhooks:** Expose callback endpoints for Twilio/SendGrid to update delivery states asynchronously.

---
**Author:** Antigravity AI
