# TicketMesh — Architecture

## 1. System Overview

TicketMesh is a modular Spring Boot backend (single deployable "core" service) fronted by
a React consumer Web + PWA, a React admin portal, an agent/shop app channel, and an
internal operations surface. It is designed for multi-tenant white-label SaaS and can be
deployed to AWS, Kubernetes/EKS, on-premises and Docker with horizontal autoscaling.

**Key architectural decisions**

- One modular backend exposing clear domain boundaries (identity, catalog, marketplace,
  booking, payment, inventory, support, fraud, ops, **analytics, globalization, commerce**) —
  easy to split into microservices later without a rewrite.
- Multi-tenant model: every tenant carries country (ISO 3166-1 alpha-2), currency
  (ISO 4217), default language (BCP-47) and timezone.
- Marketplace model (Uber/PickMe-style): agents/shops connect via app, upload their own
  products, sell to customers; customers also self-serve; admins monitor everything.
- White-label theming stored per tenant (branding entity) and served publicly so consumer
  frontends render any shop's theme purely from configuration.

## 2. Container / Component View

- **Consumer Web + PWA** — React (TypeScript), installable, mobile-responsive, renders
  tenant white-label theme from `/api/tenant/{slug}/branding`.
- **Admin Portal** — React; tenant + shop + user + catalog + support + fraud + ops dashboards.
- **Agent/Shop App** — mobile-responsive PWA for agents to onboard, upload products and
  manage their own shop.
- **ticketmesh-core (Spring Boot 3.3.5)** — REST APIs:
  - Identity & Auth (JWT)
  - Tenant & Branding (white-label config)
  - Agent Onboarding (shop apply -> approve)
  - Provider + Catalog (products, universal search)
  - Booking / Reservation / Payment / Inventory
  - Support (tickets, 24/7 portal, SLA, escalation)
  - Fraud / Risk detection + ops signals
  - **ML / analytics** (`com.ticketmesh.ml`) — demand forecast, price prediction,
    recommendations, trend report, anomaly scoring, and an NL data analyst
    (`AnalyticsController` under `/api/analytics`)
  - **Event / outbox engine** — every important business action emits an event via the
    outbox table (`EventService`, polled/published), feeding the ops event feed
  - **Promotions / loyalty / globalization** — promo engine + `/api/promotions`, loyalty
    points/tiers + `/api/loyalty`, and multi-currency / multi-language globalization
    (translate, dictionary, FX rates) + `/api/globalization`
  - **Commerce controller groups** — reviews, notifications, trips, live pricing
    (`/api/reviews`, `/api/notifications`, `/api/trips`, `/api/pricing`), plus ops/disruption
    intelligence and audit (`/api/ops`, `/api/audit`)
  - AI assistant + RAG + guardrails
  - Actuator health/readiness/metrics
- **Datastores** — MySQL/PostgreSQL (primary), Flyway migrations, H2 (test, MySQL mode).
- **Provider integration** — canonical HTTP client, WireMock-stubbed in tests.
- **Observability** — Spring Boot Actuator + Micrometer, Prometheus/Grafana.

## 3. Security

- OAuth2/JWT auth with `CUSTOMER`, `AGENT`, `ADMIN` roles; method-level authorization.
- Tenant isolation on data; passwords hashed; secrets via env vars (never committed).
- Input validation on all DTOs; global exception handling.
- Fraud detection (0-100 score, flags, auto-block, audit signals) with admin override.

## 4. Deployment Topology

- **Build**: Maven; artifacts published to a registry.
- **Runtime**: containerized app + managed DB.
- **Scaling**: stateless app -> horizontal autoscaling (HPA/KEDA on EKS, ASG/ECS on AWS);
  DB scales vertically or via managed service.
- **Targets**: AWS, EKS, on-prem, Docker, single server.

See `docs/deployment-autoscaling.md` for the full deployment guide.
