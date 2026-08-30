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

- **Marketplace (Uber / PickMe style)** — `CUSTOMER`, `AGENT`, `ADMIN` roles. Shops/agents
  connect via the app, upload and sell their own services; customers also self-serve; shop
  owners manage their own shop from the app; admins monitor everything.
- **Global white-label SaaS** — any country (ISO 3166-1), any currency (ISO 4217), any
  language (BCP-47), timezone; per-tenant theming (logo, colors, images) all by
  configuration, no code.
- **Channels** — consumer Web + mobile-responsive installable PWA; admin portal for overall
  management & monitoring.
- **Bookings, payments & QR tickets** — reservation hold/expiry, idempotent payment/refunds,
  ZXing QR signing (HMAC-SHA256) and gate verification.
- **24/7 support** — support portal with tickets, priority/SLA, assignment, escalation and
  automated SLA-breach escalation.
- **Fraud detection** — 0-100 risk scoring, named flags, auto-block, persisted signals,
  admin override.
- **AI assistant + RAG + guardrails** — conversational help with retrieval and safety
  guardrails.
- **Automation** — health checks, auto issue detection and scripted auto-fix, observability
  (Actuator/Micrometer/Prometheus).
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
| Catalog | `GET /api/catalog/search` | Universal search (public) |
| Branding | `GET /api/tenant/{slug}/branding` | White-label theme (public) |
| Booking | `POST /api/tickets/`, `POST /api/tickets/{id}/cancel`, `POST /api/tickets/verify` | |
| Support | `/api/support/tickets...` | 24/7 portal (+ admin paths) |
| Fraud | `/api/security/fraud/check`, `.../signals`, `.../high-count` | Admin |
| Health | `GET /api/health/live`, `GET /actuator/health` | public |

See **`docs/api-production.md`** and **`docs/api-test.md`** for the complete test and
production API references.

---

## Documentation set (all present in `docs/`)

- `spec/REQUIREMENTS.md` — master requirement register (single source of truth)
- `TicketMesh_Scope_Document.docx` + `.pdf` — rendered scope document
- `PLATFORM_PROMPT_TEMPLATE.md` — reusable template to build other platforms
- `architecture.md`, `sdlc-process.md`
- `release-notes-dev.md`, `release-notes-qa.md`
- `manual-consumer.md`, `manual-agent.md`, `manual-admin.md`, `manual-ops.md`
- `api-test.md`, `api-production.md`
- `deployment-autoscaling.md`

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
mvn test          # 62 tests across backend + WireMock contract + full flow
cd frontend && npm run build   # type-checks + produces the PWA
```

---

## Tech stack

| Concern | Technology |
|---------|-----------|
| Language | Java 21 (target) |
| Framework | Spring Boot 3.3.5, Spring Security, Spring Data JPA |
| Auth | JWT (JJWT 0.12.6), BCrypt |
| QR | ZXing (HMAC-SHA256 signed) |
| Migrations | Flyway |
| Database | MySQL 8 (runtime), H2 (tests, MySQL mode) |
| AI | Spring AI-like assistant via HttpClient adapters + RAG + guardrails |
| Frontend | React 18 + TypeScript + Vite + PWA |
| Testing | JUnit 5, Mockito, WireMock 3.13.2 |
| Infra | Docker, docker-compose, Kubernetes/Helm/HPA, Terraform/ECS |
| CI/CD | GitHub Actions (CI, Security, Release) |

---

## License

MIT
