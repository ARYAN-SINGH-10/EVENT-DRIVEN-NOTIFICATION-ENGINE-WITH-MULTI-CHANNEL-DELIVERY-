# 🚀 Notification Engine — Enterprise Operations Console

A professional, high-density operations console and control center for the **Event-Driven Notification Engine**, redesigned with a light-first, restrained enterprise design system (inspired by Stripe Dashboard, Linear, and GitHub).

---

## 🎨 Enterprise Design System

### Design Tokens
- **Backgrounds**: Slate-50 (`#F8FAFC`) app background, pure white (`#FFFFFF`) content surfaces.
- **Typography**: Inter / system sans-serif with tabular numerical figures (`font-feature-settings: 'tnum'`).
- **Color Accents**:
  - Primary Accent: Blue-600 (`#2563EB`) / Blue-700 hover (`#1D4ED8`)
  - Success: Green-700 (`#15803D`)
  - Warning: Amber-700 (`#B45309`)
  - Error: Red-700 (`#B91C1C`)
- **Borders & Shadows**: Thin neutral borders (`#E2E8F0`), subtle 6–8px border radii, minimal shadows (`0 1px 2px 0 rgba(0, 0, 0, 0.05)`).
- **No AI Tropes**: Zero glowing purple borders, zero glassmorphism, zero unnecessary decorative animations.

---

## 🧭 Navigation Structure

- **Overview**
  - **Dashboard**: High-density KPI strip (events processed, delivery SLA, pending DLQ, provider availability), processing lifecycle stepper, gateway health table, and recent event feed.
- **Event Processing**
  - **Event Ingestion**: Structured JSON event dispatcher with standard financial taxonomy presets (`TXNX-001`, `PROMO-001`, `CRIT-001`, `ACC-003`) and step-by-step pipeline decision trace.
  - **Notification Stream**: High-density audit table with status and channel filters, search bar, and slide-over inspection modal.
  - **Dead Letter Queue (DLQ)**: Operational failure remediation table with individual replay, mark resolved with operator notes, discard, and batch retry-all.
- **Routing & Configuration**
  - **Router & Simulator**: Composite scoring calculator (cost vs SLA) with clear failover fallback sequence and isolated provider chaos test sandbox.
  - **Template Studio**: Split-pane template registry, dynamic mustache tag editor, and multi-locale live preview.
  - **TRAI & Compliance**: Regulatory policy evaluator, interactive TRAI National DND simulator, and GDPR Article 21 right-to-erasure administration.
- **Insights**
  - **Analytics**: 8-metric SaaS performance dashboard with date-range filters (`7d`, `30d`, `90d`), channel spend comparison, and 24-hour diurnal dispatch distribution.

---

## ⚡ Dual-Mode Connectivity

1. **Production / Live Spring Boot**: Automatically detects `http://localhost:8080/api/v1` and routes requests through the Vite proxy.
2. **Local Simulation Mode**: If the Spring Boot service or Kafka cluster is offline, an integrated local state engine provides 100% interactive functionality with clear environment badging.

---

## 🛠️ Development & Build

```bash
# Run local dev server
cd frontend
npm run dev

# Run production build
npm run build
```
*(Production bundle is pre-built to `frontend/dist/` and synchronized with `src/main/resources/static/`)*.
