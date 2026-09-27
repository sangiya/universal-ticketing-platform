# TicketMesh — Universal Configurable Ticketing & Reservation Platform

> **All your tickets, one platform.**

TicketMesh is a production-grade, universally configurable, multi-provider ticketing and
reservation SaaS covering **bus, train, movie, events, sports, flight, ferry, attractions**
and future domains. It can be bought as software, self-hosted on your own server, or run as
a full managed service with hosting. It deploys to AWS, Kubernetes (EKS), on-premises and
Docker with horizontal autoscaling.

Built as a modular Spring Boot backend + a React consumer Web **and** installable PWA, plus
an admin portal and an agent/shop app channel.

---

## Highlights

- **Marketplace (Uber / PickMe style)** — `CUSTOMER`, `AGENT`, `ADMIN` roles. Any shop/agent
  can **self-register and start selling immediately** (open, instant activation) or be
  gated behind admin review — controlled per tenant by a configurable **moderation mode**
  (`INSTANT` / `REVIEW`). Shops/agents connect via the app, upload and sell their own
  services; customers also self-serve; shop owners manage their own shop from the app;
  admins monitor everything.
- **Strong onboarding security** — **2FA** with email/SMS **OTP**, **TOTP** (RFC 6238,
  JDK-only), and **app-keys**; **identity verification** (NIC / passport / driving licence +
  photo) with admin review; **PII encrypted at rest** (AES-256/GCM) with a masked-PII view.
- **Omnichannel messaging** — WhatsApp / Facebook / Telegram / SMS channel abstraction,
  public webhook ingestion, outbound send, and per-tenant channel integrations.
- **Multiple payment methods** — **CARD**, **WALLET**, **PAYPAL**, **BANK** (plus);
  offline payment-gateway abstraction.
- **Social & commerce** — **family groups**, per-user **settings** (theme/language/
  currency/notification prefs), **referrals & invite friends** (referral codes + loyalty
  rewards), **vouchers & offers**, loyalty points/tiers, product reviews and notifications.
- **Global white-label SaaS** — any country (ISO 3166-1), any currency (ISO 4217), any
  language (BCP-47), timezone; per-tenant theming (logo, colors, images) all by
  configuration, no code. White-label to run on your own VM/cloud **or** as a hosted
  managed service.
- **Channels** — consumer Web + mobile-responsive installable PWA; admin portal for overall
  management & monitoring; agent/shop app with white-label.
- **Bookings, payments & QR tickets** — reservation hold/expiry, idempotent payment/refunds,
  ZXing QR signing (HMAC-SHA256) and gate verification.
- **24/7 support** — support portal with tickets, priority/SLA, assignment, escalation and
  automated SLA-breach escalation.
- **Fraud detection** — 0-100 risk scoring, named flags, auto-block, persisted signals,
  admin override.
- **AI assistant + RAG + guardrails** — conversational help with retrieval and safety
  guardrails.
- **ML / analytics + NL data analyst** — demand forecast, price prediction,
  recommendations, trend report, anomaly scoring, plus a natural-language data analyst
  (`/api/analytics`).
- **Marketplace commerce** — promotions, loyalty points/tiers, product reviews,
  notifications and multi-leg trips.
- **Globalization** — multi-currency (ISO 4217) FX rates and multi-language (BCP-47)
  translation dictionaries via `/api/globalization`.
- **Disruption intelligence** — active-disruption reports with findings and recovery
  recommendations (`/api/ops/disruption`), plus an outbox event feed and audit log.
- **Automation** — health checks, auto issue detection and scripted auto-fix, observability
  (Actuator/Micrometer/Prometheus).
- **Deploy anywhere** — Windows, Linux, Docker/docker-compose, Kubernetes/Helm, or any
  cloud (AWS ECS via Terraform or a generic VPS); self-host or managed.
- **Edge & platform surface** — config-driven multi-domain verticals, explicit capability
  matrix, no-code product templates, data-platform streaming ingest, and edge rate limiting
  + JVM observability — all under `/api/platform/**` and `/api/edge/**`.
- **Offline-friendly** — H2 in MySQL mode for tests; WireMock stubs for provider/API calls.

---

## Repository layout

```
universal-ticketing-platform/
├── src/                  ─ Spring Boot 3.3.5 backend (ticketmesh-core)
├── frontend/             ─ React + TypeScript PWA + admin portal
├── docs/                 ─ full document set (scope, SDLC, release notes, manuals, API)
├── infra/
│   ├── kubernetes/       ─ Deployment, Service, HPA, ConfigMap (autoscaling)
│   ├── helm/ticketmesh/  ─ Helm chart with HPA + secrets
│   └── terraform/        ─ AWS ECS Fargate + service auto scaling
├── .github/workflows/    ─ CI, Security, Release
├── docker-compose.yml    ─ MySQL + app stack
├── Dockerfile
└── tools/generate_docs.py─ renders the scope .docx/.pdf from docs/spec/REQUIREMENTS.md
```

---

## Quick start

Prerequisites: **Java 21**, **Maven 3.9+**, **Docker** (MySQL).

```bash
# 1. Start MySQL
docker compose up -d mysql

# 2. Build + run the full test suite
mvn test

# 3. Run the backend (http://localhost:8080)
mvn spring-boot:run

# 4. Run the frontend PWA + admin (http://localhost:5173)
cd frontend && npm install && npm run dev
```

### Bootstrap admin

`admin` / `ChangeMe123!` (override `BOOTSTRAP_ADMIN_USERNAME` / `BOOTSTRAP_ADMIN_PASSWORD`
in production). Always change defaults in production.

---

## API overview (summary)

| Area | Path | Notes |
|------|------|-------|
| Auth | `POST /api/auth/register`, `POST /api/auth/login` | JWT |
| Catalog | `GET /api/catalog/search`, `GET /api/catalog`, `GET /api/catalog/{id}` | Universal search (public) |
| Branding | `GET /api/tenant/{slug}/branding` | White-label theme (public) |
| Booking | `GET /api/bookings/{id}`, `POST /api/bookings/{id}/cancel`, `GET /api/tickets/booking/{bookingId}`, `GET /api/tickets/verify` (public) | |
| Payments | `POST /api/payments/booking/{bookingId}`, `POST /api/payments/{paymentId}/settle`, `GET /api/payments/booking/{bookingId}/status` | CARD / WALLET / PAYPAL / BANK |
| Support | `/api/support/tickets...` | 24/7 portal (+ admin paths) |
| Fraud | `/api/security/fraud/check`, `.../signals`, `.../high-count` | Admin |
| Payments/security | `POST /api/security/fraud/check`, `GET /api/security/fraud/signals`, `GET /api/security/fraud/high-count` | Admin |
| 2FA / profile | `/api/security/profile/2fa/otp/send`, `.../2fa/otp/verify`, `.../2fa/totp/enable`, `.../2fa/totp/verify`, `.../appkey/issue`, `.../appkey/verify` | Auth |
| Identity | `POST /api/identity/verify`, `GET /api/identity/me`, `POST /api/admin/identity/{id}/review` | Auth / ADMIN |
| PII | `GET /api/security/pii/me` | Masked PII (auth) |
| Analytics/ML | `/api/analytics/forecast`, `.../price`, `.../recommend`, `.../trend`, `.../anomaly`, `.../ask`, `POST /api/ai/assistant` | Auth |
| Commerce | `/api/promotions`, `/api/loyalty`, `/api/reviews`, `/api/notifications`, `/api/trips`, `/api/pricing/{productId}`, `/api/orders` | Auth (promotions: PERCENT / FLAT / VOUCHER / OFFER) |
| Social | `/api/family`, `/api/referrals`, `/api/referrals/invite`, `/api/referrals/validate` (public), `/api/settings` | Auth |
| Messaging | `POST /api/messaging/webhook/tenant/{id}/channel/{ch}` (public), `POST /api/messaging/send`, `GET /api/messaging`, `POST /api/messaging/channels/{tenantId}` | WhatsApp / Facebook / Telegram / SMS |
| Globalization | `/api/globalization/translate`, `.../dictionary`, `.../rates`, `.../currencies`, `.../languages` | Auth |
| Agent | `/api/agent/shops`, `/api/agent/shops/me`, `/api/agent/providers`, `/api/agent/products`, `/api/agent/providers/{code}/products` | AGENT |
| Admin/Tenant | `/api/admin/dashboard`, `/api/admin/shops`, `/api/admin/tenants` (+ `/status`, `/moderation`, `/branding`), `/api/admin/providers` | ADMIN |
| Ops | `/api/ops/events`, `/api/ops/disruption`, `/api/audit` | Admin |
| Health | `GET /api/health/live`, `GET /actuator/health` | public |
| Platform/Config | `GET /api/platform/domains`, `.../capabilities?domain=`, `.../product-templates?kind=` | Vertical catalog, capability matrix, no-code templates (auth) |
| Edge/Observability | `GET /api/edge/health`, `GET /api/edge/metrics`, `POST /api/platform/ingest` | Rate-limit probe, JVM metrics, data-platform ingest (auth) |
| AI surface | `GET /api/ai/search?q=`, `GET /api/ai/tools`, `GET /api/trips/plan`, `GET /api/analytics/seat`, `.../dynamic-price` | Conversational search, agent tools, trip planner, seat + surge (auth) |

See **`docs/api-production.md`** and **`docs/api-test.md`** for the complete test and
production API references.

---

## Documentation set (all present in `docs/`)

- `spec/REQUIREMENTS.md` — master requirement register (single source of truth)
- `TicketMesh_Scope_Document.docx` + `.pdf` — rendered scope document
- `PLATFORM_PROMPT_TEMPLATE.md` — reusable template to build other platforms
- `architecture.md`, `architecture-graphs.md`, `tech-matrix.md`, `global-benchmark.md`, `sdlc-process.md`
- `release-notes-dev.md`, `release-notes-qa.md`
- `manual-consumer.md`, `manual-agent.md`, `manual-admin.md`, `manual-ops.md`
- `api-test.md`, `api-production.md`
- `deployment-autoscaling.md`
- `self-host-deployment.md`, `white-label-guide.md`, `security-privacy-guide.md`

Regenerate the scope `.docx`/`.pdf` with: `python tools/generate_docs.py`.

---

## Infrastructure & autoscaling

- **Kubernetes/EKS** — `infra/kubernetes/deployment.yaml` + `hpa.yaml` (CPU/memory HPA,
  min 2 / max 20, fast scale-up, stabilized scale-down, readiness + liveness probes,
  non-root read-only pods).
- **Helm** — `infra/helm/ticketmesh` packages the app with HPA, Service, secrets and
  ConfigMap.
- **AWS** — `infra/terraform` provisions ECS Fargate with target-tracking service auto
  scaling, CloudWatch logs and IAM least-privilege.
- **Docker** — `Dockerfile` (multi-stage, non-root) + `docker-compose.yml` (MySQL + app).

---

## Testing

```bash
mvn test          # 204 tests: unit + integration + WireMock contract + full flow
cd frontend && npm run build   # type-checks + produces the PWA
```

---

## Tech stack

| Concern | Technology |
|---------|-----------|
| Language | Java 21 (target) |
| Framework | Spring Boot 3.3.5, Spring Security, Spring Data JPA |
| Authentication | JWT (JJWT 0.12.6), BCrypt, **2FA (OTP / TOTP RFC 6238 JDK-only / app-keys)** |
| Identity & PII | Identity verification (NIC / passport / driving licence), **AES-256/GCM encryption at rest**, masked-PII view |
| Messaging | WhatsApp / Facebook / Telegram / SMS adapters (public webhook + outbound) |
| Payments | **CARD / WALLET / PAYPAL / BANK** offline gateway abstraction |
| Social / commerce | Family groups, per-user settings, referrals + loyalty rewards, vouchers/offers |
| QR | ZXing (HMAC-SHA256 signed) |
| Migrations | Flyway |
| Database | MySQL 8 (runtime), H2 (tests, MySQL mode) |
| AI | LLM assistant via HttpClient adapters + RAG + guardrails (offline-first) |
| ML/Analytics | Deterministic ML-style suite (forecast, price, recommend, anomaly, trend) + NL data analyst |
| Frontend | React 18 + TypeScript + Vite + PWA |
| Testing | JUnit 5, Mockito, WireMock 3.13.2 |
| Infra | Docker, docker-compose, Kubernetes/Helm/HPA, Terraform/ECS, Windows/Linux/VPS |
| CI/CD | GitHub Actions (CI, Security, Release) |

---

## License

MIT

## Open Source Contributions

- **scorelab-Ticket** · Ticket management web application (Java EE/JSP + SQL, SCORE Lab open-source project)
