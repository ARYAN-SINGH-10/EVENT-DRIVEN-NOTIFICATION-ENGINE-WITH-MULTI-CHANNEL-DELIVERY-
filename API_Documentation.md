# API Documentation

The Event-Driven Notification Engine exposes RESTful JSON APIs for ingestion, administration, preference management, and analytics.

**Base URL:** `http://localhost:8080`
**Content-Type:** `application/json`

---

## 1. Events API
Endpoints for ingesting raw domain events.

### `POST /api/v1/events`
Ingests a new domain event into the notification pipeline.

**Request Body:**
```json
{
  "eventType": "TXNX-001",
  "userId": "usr_99812",
  "source": "payment-gateway",
  "correlationId": "corr-771829",
  "priority": "HIGH",
  "data": {
    "amount": "150.00",
    "currency": "USD",
    "merchant": "Amazon"
  }
}
```

**Response (200 OK):**
```json
{
  "eventId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "status": "ACCEPTED",
  "message": "Event queued successfully."
}
```

**Error (400 Bad Request):**
```json
{
  "error": "eventType is required"
}
```

---

## 2. User Preferences API
Manage user opt-ins, channels, and quiet hours.

### `POST /api/v1/preferences`
Save or update a preference.

**Request Body:**
```json
{
  "userId": "usr_99812",
  "eventType": "PROMO-001",
  "channel": "SMS",
  "enabled": false,
  "quietHoursStart": "22:00",
  "quietHoursEnd": "08:00",
  "timezone": "America/New_York"
}
```

**Response (200 OK):**
```json
{
  "id": "e44...79",
  "userId": "usr_99812",
  "enabled": false
}
```

### `GET /api/v1/preferences/{userId}`
Retrieves all preferences for a user.

---

## 3. Dashboard API
Retrieves aggregate platform health and metrics.

### `GET /api/v1/dashboard`
**Response (200 OK):**
```json
{
  "totalNotificationsProcessed": 145020,
  "overallDeliveryRate": 98.4,
  "activeChannels": ["SMS", "EMAIL", "PUSH"],
  "dlqSize": 142,
  "systemHealth": "HEALTHY",
  "metrics": {
    "SMS": {
      "totalDelivered": 85000,
      "totalFailed": 100,
      "costEstimate": 125.50
    }
  }
}
```

---

## 4. Analytics API
Fetch granular time-series and rollup metrics.

### `GET /api/v1/analytics/summary?channel=SMS`
Fetches a high-level summary of analytics.

### `GET /api/v1/analytics/delivery-rate?channel=EMAIL`
Returns delivery percentage.

---

## 5. Dead Letter Queue (DLQ) API
Manage failed notifications.

### `GET /api/v1/dlq?page=0&size=10`
Returns paginated DLQ items.

### `POST /api/v1/dlq/{notificationId}/retry`
Forces an immediate retry of a DLQ item.

---

## Authentication Notes
*(Implementation Details)*
Currently, the APIs are open for internal microservice consumption. In a production environment, it is assumed that these endpoints are secured behind an API Gateway (like Spring Cloud Gateway or Kong) utilizing OAuth2 / JWT authentication, or protected via Spring Security.
