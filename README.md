# 🚀 Event-Driven Notification Engine with Multi-Channel Delivery

![Java](https://img.shields.io/badge/Java-21-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-8+-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-Latest-231F20?style=for-the-badge&logo=apachekafka&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger-OpenAPI%203-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)
![Build](https://img.shields.io/badge/Build-SUCCESS-brightgreen?style=for-the-badge)
![Tests](https://img.shields.io/badge/Tests-60%20Passing-brightgreen?style=for-the-badge)

---

## 📖 Overview

The **Event-Driven Notification Engine** is a production-grade backend platform built using **Spring Boot 3** and **PostgreSQL** that acts as an intelligent middleware between domain event producers and end-user communication channels.

The system provides:

- 📡 Multi-channel notification delivery (SMS, Email, Push, WhatsApp, Slack, In-App, Voice, Webhooks)
- 🧠 Intelligent cost-optimized channel routing with fallback awareness
- ⚡ Apache Kafka-based asynchronous event streaming (with simulated mode)
- 🛡️ Compliance engine with TRAI DND enforcement
- ⏰ Frequency capping & quiet-hour queueing
- 💀 Dead Letter Queue (DLQ) with configurable retry backoff
- 🔄 Full state machine with immutable audit logging
- 📊 Async analytics & operational dashboard
- 📚 OpenAPI / Swagger UI documentation

The platform is designed to reliably deliver the **right notification**, through the **right channel**, at the **right time** — while strictly obeying compliance boundaries.

---

## 🏗️ Architecture Overview

```
Domain Event Producers (REST / Kafka)
              │
              ▼
   Event Ingestion Pipeline
              │
     ┌────────┼────────┐
     ▼        ▼        ▼
Compliance  Frequency  Quiet Hour
  Engine    Capper     Queue
     └────────┼────────┘
              ▼
   Intelligent Channel Router
              │
  ┌─────┬────┼────┬──────┬──────┐
  ▼     ▼    ▼    ▼      ▼      ▼
 SMS  Email Push WhatsApp Slack  Voice
              │
  ┌───────────┼───────────┐
  ▼           ▼           ▼
PostgreSQL  Kafka DLQ  Audit Log
```

---

## ✨ Key Features

### 📡 Multi-Channel Delivery

Pluggable channel processors supporting:

- **SMS** — Transactional & OTP delivery
- **Email** — Rich templated messaging
- **Push Notifications** — Mobile & web push
- **WhatsApp** — Conversational alerts
- **Slack** — Internal ops notifications
- **In-App** — Real-time embedded alerts
- **Voice** — Critical outbound calls
- **Webhooks** — Event forwarding to external systems

### 🧠 Intelligent Channel Router

- Cost-optimized channel selection
- Fallback-aware routing (primary → secondary → tertiary)
- Per-user preference resolution
- Event taxonomy-aware routing rules

### 🚦 Financial Event Taxonomy & Compliance

All events are strictly classified under the `FinancialEventType` taxonomy:

| Code | Category | DND | Quiet Hours | Frequency Cap |
|------|----------|-----|-------------|---------------|
| `TXNX-001` | Transactional (OTP, Payment) | Bypasses | Respects | Respects |
| `PROMO-001` | Promotional (Loan Offers) | Blocked | Blocked | Blocked |
| `CRIT-001` | Critical / Fraud Alerts | Bypasses | Bypasses | Bypasses |

### ⚡ Apache Kafka Event Streaming

- Kafka producer/consumer pipeline with topic-based routing
- **Simulation mode** available — no Kafka installation required for local dev
- Dead Letter Topic (DLT) for failed message routing

### 💀 Dead Letter Queue (DLQ)

Failed notification events are:

- Queued with exponential backoff retries
- Moved to DLQ after retry exhaustion
- Replayable through dedicated APIs

### ⏰ Quiet Hours & Frequency Capping

- **Quiet Hours**: Timezone-aware message queueing during user sleep windows
- **Frequency Capping**: Velocity limits at global, channel, and taxonomy level
- **Critical events** bypass all limits automatically

### 🔄 Notification State Machine

Supported states:

```
CREATED → COMPLIANCE_CHECK → SCHEDULED / BLOCKED
SCHEDULED → DISPATCHED → DELIVERED / FAILED
FAILED → RETRYING → DLQ
```

Features:
- Strict transition validation
- Immutable audit log for every state change
- Full lifecycle traceability

### 📊 Analytics & Dashboard

Asynchronous metric aggregation APIs for:

- Delivery Rate & Click-Through Rate (CTR)
- Cost-Per-Notification analytics
- Channel health monitoring
- DLQ backlog reporting

---

## 📚 Swagger / OpenAPI & Accessing the App

> **Note:** This is a pure backend API — there is no HTML frontend at the root URL (`/`). Visiting `/` will return a standard **Whitelabel Error Page (404)** — this is expected!

To interact with the application, access the **Swagger UI**:

### 🖥️ Local Environment
```
http://localhost:8080/swagger-ui/index.html
```

### 🌐 Production Environment (e.g., Render / Railway)
```
https://your-app-name.onrender.com/swagger-ui/index.html
```

### 📄 Raw OpenAPI JSON
```
http://localhost:8080/v3/api-docs
```

---

## 🛠️ Technology Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Core language |
| Spring Boot | 3.3.5 | Application framework |
| Maven | 3.9+ | Build tool |
| PostgreSQL | 8+ | Primary persistence |
| Apache Kafka | Latest | Async event streaming |
| Spring Data JPA / Hibernate | Latest | ORM & data access |
| Caffeine Cache | Latest | In-memory caching |
| Lombok | 1.18.34 | Boilerplate reduction |
| Springdoc OpenAPI | 2.6.0 | Swagger UI |
| JUnit 5 / Mockito | Latest | Testing |
| Docker | Latest | Containerization |

---

## 📂 Project Structure

```
event-driven-notification-engine
│
├── src
│   ├── main
│   │   ├── java/com/notificationengine
│   │   │   ├── analytics/        # Metric aggregation & rollup schedulers
│   │   │   ├── channel/          # Channel interfaces & processors
│   │   │   ├── compliance/       # DND & TRAI compliance checks
│   │   │   ├── config/           # Spring configs (Kafka, Cache, etc.)
│   │   │   ├── controller/       # REST APIs
│   │   │   ├── dlq/              # Dead Letter Queue logic
│   │   │   ├── dto/              # Data Transfer Objects
│   │   │   ├── entity/           # JPA Entities
│   │   │   ├── event/            # Ingestion pipeline, Kafka producers/consumers
│   │   │   ├── exception/        # Global exception handling
│   │   │   ├── personalization/  # Financial calculators & personalization
│   │   │   ├── preference/       # User preference resolution
│   │   │   ├── provider/         # Notification provider integrations
│   │   │   ├── repository/       # JPA Repositories
│   │   │   ├── scheduler/        # Scheduled jobs
│   │   │   ├── service/          # Core business logic & state machine
│   │   │   ├── taxonomy/         # Financial event enums
│   │   │   ├── template/         # Template rendering engine
│   │   │   └── util/             # Utility helpers
│   │
│   └── resources
│       └── application.properties
│
├── src/test/java
├── pom.xml
├── Dockerfile
├── README.md
├── Architecture.md
├── DatabaseSchema.md
├── API_Documentation.md
└── TestingGuide.md
```

---

## 🗄️ Database Setup

Create the PostgreSQL database:

```sql
CREATE DATABASE notification_db;
```

Update `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/notification_db
spring.datasource.username=your_username
spring.datasource.password=your_password
```

> **Tip:** For quick local development, the app defaults to an **H2 in-memory database** — no setup needed!

---

## ▶️ Running the Application

### Build

```bash
mvn clean install
```

### Run

```bash
mvn spring-boot:run
```

Application runs at:

```
http://localhost:8080
```

### Run with Docker

```bash
docker build -t notification-engine .
docker run -p 8080:8080 notification-engine
```

---

## 🔍 API Highlights

### Events
```
POST   /api/v1/events           # Ingest a domain event
GET    /api/v1/events/{id}      # Get event status
```

### Notifications
```
GET    /api/v1/notifications/{id}          # Get notification details
POST   /api/v1/notifications/{id}/retry    # Retry a failed notification
```

### Dead Letter Queue
```
GET    /api/v1/dlq              # List DLQ entries
POST   /api/v1/dlq/{id}/replay  # Replay a DLQ message
```

### Analytics & Dashboard
```
GET    /api/v1/dashboard        # Operational dashboard summary
GET    /api/v1/analytics/stats  # Delivery & CTR statistics
```

### User Preferences
```
GET    /api/v1/preferences/{userId}    # Get user channel preferences
PUT    /api/v1/preferences/{userId}    # Update preferences
```

---

## 🧪 Testing

Run all tests:

```bash
mvn clean test
```

Generate Surefire HTML report:

```bash
mvn surefire-report:report
```

Current status:

```
Tests Run : 60
Failures  : 0
Errors    : 0
Build     : SUCCESS
```

*Report available at: `target/site/surefire-report.html`*

---

## 📑 Additional Documentation

- [Architecture.md](./Architecture.md) — Deep-dive into pipeline, Kafka flows & state machine
- [DatabaseSchema.md](./DatabaseSchema.md) — Full entity-relationship diagram and schema
- [API_Documentation.md](./API_Documentation.md) — Offline API reference
- [TestingGuide.md](./TestingGuide.md) — Testing strategy and scenarios

---

## 🚀 Future Improvements

- Redis-based distributed frequency capping for multi-instance scaling
- GraphQL API layer for the analytics dashboard
- Provider webhooks for Twilio/SendGrid async delivery updates
- OpenTelemetry distributed tracing
- Kubernetes deployment manifests
- ML-driven channel selection optimization

---

## 💻 Author

**Aryan Singh**
