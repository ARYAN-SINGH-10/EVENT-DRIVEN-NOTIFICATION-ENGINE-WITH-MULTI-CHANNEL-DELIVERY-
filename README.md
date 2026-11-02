# 🚀 Event-Driven Notification Engine with Multi-Channel Delivery

![Java](https://img.shields.io/badge/Java-21-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![Vite](https://img.shields.io/badge/Vite-8-646CFF?style=for-the-badge&logo=vite&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-Stream-231F20?style=for-the-badge&logo=apachekafka&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Render](https://img.shields.io/badge/Render-Deployed-46E3B7?style=for-the-badge&logo=render&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger-OpenAPI%203-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)
![Tests](https://img.shields.io/badge/Tests-60%20Passing-brightgreen?style=for-the-badge)

---

## 🌐 Live Production Deployments (Render)

| Component | Platform | Live URL |
|---|---|---|
| **🎨 Operations Dashboard (React UI)** | Render Static Site | **[https://notification-engine-frontend.onrender.com](https://notification-engine-frontend.onrender.com)** |
| **⚡ Backend REST API** | Render Docker Web Service | **[https://notification-engine-backend-7bts.onrender.com](https://notification-engine-backend-7bts.onrender.com)** |
| **📚 Interactive Swagger UI** | OpenAPI 3 | **[https://notification-engine-backend-7bts.onrender.com/swagger-ui.html](https://notification-engine-backend-7bts.onrender.com/swagger-ui.html)** |
| **📄 OpenAPI JSON Schema** | OpenAPI 3 Spec | **[https://notification-engine-backend-7bts.onrender.com/v3/api-docs](https://notification-engine-backend-7bts.onrender.com/v3/api-docs)** |

> [!NOTE]
> On Render's Free Tier, instances spin down after 15 minutes of inactivity. If visiting for the first time in a while, the backend may take 30–50 seconds to complete its initial cold start.

---

## 📖 Overview

The **Event-Driven Notification Engine** is an enterprise-grade notification platform built using **Spring Boot 3 (Java 21)** and **React 19 (Vite)** that acts as an intelligent middleware between domain event producers and end-user communication channels.

The system provides:

- 📡 **Multi-channel delivery** (SMS, Email, Push, WhatsApp, Slack, In-App, Voice, Webhooks)
- 🧠 **Intelligent cost-optimized channel routing** with fallback awareness
- ⚡ **Apache Kafka-based asynchronous event streaming** (with built-in simulated fallback)
- 🛡️ **Compliance engine** with TRAI DND (Do Not Disturb) and GDPR regulations
- ⏰ **Frequency capping & quiet-hour queueing**
- 💀 **Dead Letter Queue (DLQ)** with automated retry backoff & manual operator triage
- 🔄 **Full state machine** with immutable audit logging
- 🎨 **Modern Operations Dashboard** with real-time analytics and event simulation
- 📚 **OpenAPI 3 / Swagger UI** interactive documentation

---

## 🏗️ Architecture Overview

```
Domain Event Producers (REST API / Kafka Stream)
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
        PostgreSQL  Kafka DLQ  Audit Trail
```

---

## 🖥️ Operations Dashboard (Frontend)

The included **React Operations Dashboard** ([Live UI](https://notification-engine-frontend.onrender.com)) provides 8 dedicated operational views:

1. **📊 Operations Dashboard (`DashboardView`)**: Real-time throughput KPIs, delivery success SLA progress, provider gateway availability, and active alert triage.
2. **🚀 Event Ingestion Simulator (`EventIngestionView`)**: Interactive simulator to test domain events (`TXNX-001`, `PROMO-001`, `CRIT-001`) with live payload editing and policy validation.
3. **📋 Notification Activity & Audit (`NotificationsView`)**: Filterable ledger across all channels and statuses (`DELIVERED`, `DND`, `QUIET`, `DLQ`) with full event lifecycle breadcrumbs.
4. **💀 Dead Letter Queue Hub (`DlqView`)**: Triage portal for failed notifications with one-click replay, batch re-queueing, resolution notes, and discard actions.
5. **🔀 Router & Failover Sandbox (`RouterSandboxView`)**: Simulation tool to toggle provider health (e.g. simulate SMS outage) and observe dynamic failover to WhatsApp or Email.
6. **📝 Notification Templates Studio (`TemplatesView`)**: Multi-lingual Mustache template editor with live variable preview across SMS, Email, Push, and WhatsApp.
7. **🛡️ TRAI & Compliance Policy Hub (`ComplianceView`)**: Regulatory verification tool simulating TRAI National DND enforcement and GDPR consent registry.
8. **📈 8-Metric Analytics Rollup (`AnalyticsView`)**: 30-day performance rollups covering CTR, read rates, failure rates, provider costs, and delivery latencies.

---

## ✨ Key Backend Capabilities

### 📡 Multi-Channel Delivery

Pluggable channel processors supporting:

- **SMS** — Transactional & OTP delivery (Twilio / Sinch)
- **Email** — Rich templated messaging (SendGrid Enterprise)
- **Push Notifications** — Mobile & web push (Firebase Cloud Messaging)
- **WhatsApp** — Conversational alerts (Meta Business Cloud API)
- **Slack** — Internal ops notifications (Webhooks)
- **In-App** — Real-time embedded alerts (Engine WebSocket broker)
- **Voice** — Critical outbound calls (Amazon Connect IVR)
- **Webhooks** — Event forwarding to external partner systems

### 🧠 Intelligent Channel Router

- Cost-optimized channel selection based on real-time provider tariffs
- Fallback-aware routing (primary $\rightarrow$ secondary $\rightarrow$ tertiary)
- Per-user preference resolution & communication overrides
- Financial taxonomy-aware routing rules

### 🚦 Financial Event Taxonomy & Compliance

All events are strictly classified under the `FinancialEventType` taxonomy:

| Code | Category | DND Policy | Quiet Hours | Frequency Cap |
|------|----------|------------|-------------|---------------|
| `TXNX-001` | Transactional (OTP, Payment) | Bypasses | Respects | Respects |
| `PROMO-001` | Promotional (Loan Offers) | Blocked | Blocked | Blocked |
| `CRIT-001` | Critical / Fraud Alerts | Bypasses | Bypasses | Bypasses |
| `AUTH-002` | Security / Two-Factor Login | Bypasses | Bypasses | Bypasses |
| `ACC-003` | Account Balance Summary | Blocked | Respects | Respects |

### ⚡ Apache Kafka Event Streaming

- Topic-based asynchronous event routing
- **Simulation mode** enabled by default — no local Kafka installation required
- Dead Letter Topic (DLT) for unprocessable message isolation

### 💀 Dead Letter Queue (DLQ)

Failed notification events are:
- Retried with exponential backoff and jitter
- Automatically routed to DLQ upon retry exhaustion
- Replayable through REST APIs and the Operations Dashboard

### 🔄 Notification State Machine

```
CREATED → COMPLIANCE_CHECK → SCHEDULED / BLOCKED
SCHEDULED → DISPATCHED → DELIVERED / FAILED
FAILED → RETRYING → DLQ
```

- Strict transition validation enforced by Spring State Machine logic
- Immutable audit log tracking every status transition with correlation IDs

---

## 🛠️ Technology Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 (LTS) | Core backend language |
| Spring Boot | 3.3.5 | Backend application framework |
| React | 19.2 | Operations Dashboard UI |
| Vite | 8.3 | Frontend build system |
| PostgreSQL | 15+ | Production relational persistence |
| H2 Database | 2.x | Local development zero-setup persistence |
| Apache Kafka | 3.x | Asynchronous event streaming |
| Spring Data JPA / Hibernate | 6.x | ORM & data access |
| Caffeine Cache | 3.x | High-throughput in-memory caching |
| Springdoc OpenAPI | 2.6.0 | Swagger UI & OpenAPI 3 documentation |
| Docker | Multi-stage | Containerization for cloud deployment |
| Render | Cloud | Managed PaaS hosting (Docker + Global CDN) |

---

## 📂 Project Structure

```
Event_Driven_Notification_Engine
│
├── frontend/                        # React 19 + Vite Operations Dashboard
│   ├── src/
│   │   ├── components/              # 8 Dashboard Views, Navbar, Sidebar
│   │   ├── services/api.js          # API Client with live & mock resilience
│   │   └── App.jsx                  # Main application shell
│   ├── package.json
│   └── vite.config.js
│
├── src/
│   ├── main/
│   │   ├── java/com/notificationengine/
│   │   │   ├── analytics/           # Metric aggregation & rollup schedulers
│   │   │   ├── channel/             # Multi-channel delivery processors
│   │   │   ├── compliance/          # TRAI DND & GDPR compliance engines
│   │   │   ├── config/              # Security, Kafka, Cache, Swagger config
│   │   │   ├── controller/          # 15 REST API Controllers
│   │   │   ├── dlq/                 # Dead Letter Queue logic & services
│   │   │   ├── entity/              # JPA domain entities
│   │   │   ├── event/               # Kafka producers, consumers, ingestion
│   │   │   ├── exception/           # Global RFC-7807 error handling
│   │   │   ├── preference/          # User preference resolution
│   │   │   ├── provider/            # Notification gateway providers
│   │   │   ├── repository/          # Spring Data JPA repositories
│   │   │   ├── service/             # Business logic & state machine
│   │   │   └── template/            # Mustache templating engine
│   │   └── resources/
│   │       ├── application.properties       # Default local configuration
│   │       ├── application-prod.properties  # Production configuration
│   │       └── db/migration/                # Flyway schema migrations
│   └── test/java/                   # 60 automated unit & integration tests
│
├── Dockerfile                       # Multi-stage production container build
├── render.yaml                      # 1-Click Render Cloud Blueprint
├── pom.xml                          # Maven build dependencies
├── API_Documentation.md             # Offline API Reference
├── Architecture.md                  # Deep-dive architectural specification
├── DatabaseSchema.md                # Relational schema & indexing guide
└── PRODUCTION_HARDENING_GUIDE.md    # Production deployment & security guide
```

---

## 💻 Local Quickstart

### 1. Run Backend (Spring Boot 3)
```bash
# Clone the repository
git clone https://github.com/ARYAN-SINGH-10/EVENT-DRIVEN-NOTIFICATION-ENGINE-WITH-MULTI-CHANNEL-DELIVERY-.git
cd EVENT-DRIVEN-NOTIFICATION-ENGINE-WITH-MULTI-CHANNEL-DELIVERY-

# Run with Maven (uses embedded H2 and Kafka simulation by default)
./mvnw spring-boot:run
```
*Backend runs at: `http://localhost:8080`*

### 2. Run Frontend Dashboard (Vite)
```bash
cd frontend
npm install
npm run dev
```
*Frontend runs at: `http://localhost:5173`*

---

## 🔍 API Highlights

### Events Ingestion
```http
POST /api/v1/events
Content-Type: application/json

{
  "eventType": "TXNX-001",
  "userId": "usr_sarah_99",
  "source": "payment-gateway",
  "priority": "HIGH",
  "data": {
    "amount": "250.00",
    "currency": "USD",
    "merchant": "Apple Store"
  }
}
```

### Dead Letter Queue (DLQ)
```http
GET  /api/v1/dlq              # List all queued failed events
POST /api/v1/dlq/{id}/replay  # Replay a specific dead-letter message
GET  /api/v1/dlq/stats        # Get DLQ retry backlog statistics
```

### Operations Dashboard & Health
```http
GET /api/v1/dashboard         # Real-time pipeline health & throughput
GET /api/v1/analytics/stats   # 8-metric delivery & cost rollup
```

---

## 🧪 Testing & Quality Assurance

Run the comprehensive test suite (Unit, Integration, Kafka Simulation):

```bash
mvn clean test
```

Current test status:
```
[INFO] Tests run: 60, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 📑 Additional Documentation

- [Architecture.md](./Architecture.md) — Detailed pipeline stages, failover algorithm, and Kafka topics
- [DatabaseSchema.md](./DatabaseSchema.md) — 15 relational tables, indexes, and foreign key constraints
- [API_Documentation.md](./API_Documentation.md) — Complete REST endpoint documentation
- [PRODUCTION_HARDENING_GUIDE.md](./PRODUCTION_HARDENING_GUIDE.md) — Security checklist, Flyway migrations, and environment variables

---

## 👨‍💻 Author

**Aryan Singh**
GitHub: [@ARYAN-SINGH-10](https://github.com/ARYAN-SINGH-10)
