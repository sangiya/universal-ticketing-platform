# TicketMesh — Architecture

> **Graphical views** (context / container / component / runtime / deployment diagrams):
> see `docs/architecture-graphs.md`. **Technology decisions**: see `docs/tech-matrix.md`.
> **Competitive positioning**: see `docs/global-benchmark.md`.

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
  - Identity & Auth (JWT, 2FA — OTP / TOTP RFC 6238 / app-keys)
  - Identity verification (NIC / passport / driving licence + admin review) and
    **PII encryption at rest** (AES-256/GCM) + masked-PII view
  - Tenant & Branding (white-label config, **moderation mode** INSTANT/REVIEW)
  - Agent Onboarding (open self-registration with **instant activation**)
  - Provider + Catalog (products, universal search)
  - Booking / Reservation / Payment (CARD / WALLET / PAYPAL / BANK gateways) / Inventory
  - **Omnichannel messaging** (WhatsApp / Facebook / Telegram / SMS webhooks + outbound)
  - **Social** — family groups, per-user settings, referrals + loyalty rewards
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
  - **Platform config surface** — config-driven multi-domain verticals, explicit capability
    matrix and no-code product templates, all read-only under `/api/platform/**`
    (`DomainRegistry`, `CapabilityCatalog`, `ProductCatalogBuilder`);
    `/api/platform/ingest` is the data-platform streaming read-side (outbox/event-sink
    style, `EventStreamService`, `V9`)
  - **Edge / gateway surface** — fixed-window edge rate limiting + gateway-style probes
    under `/api/edge/**` (`/api/edge/health`, `/api/edge/metrics` JVM snapshot)
  - **AI capability endpoints** — conversational search with intent detection
    (`/api/ai/search`), per-role agent tool registry (`/api/ai/tools`), automated trip
    planner (`/api/trips/plan`), AI seat recommendation and dynamic pricing
    (`/api/analytics/seat`, `/api/analytics/dynamic-price`)
  - AI assistant + RAG + guardrails
  - Observability — Actuator health/readiness/metrics + `TraceService` trace/span context
    (dependency-free; OTel/Jaeger export is a documented gateway-side option)
- **Datastores** — MySQL/PostgreSQL (primary), Flyway migrations (`V1..V9` — V7/V8 add
  security/messaging and social/PII schemas, V9 adds the data-platform `ingest_events`
  stream), H2 (test, MySQL mode).
- **Provider integration** — canonical HTTP client, WireMock-stubbed in tests.
- **Observability** — Spring Boot Actuator + Micrometer, Prometheus/Grafana.

## 3. Security

- OAuth2/JWT auth with `CUSTOMER`, `AGENT`, `ADMIN` roles; method-level authorization.
- **2FA** — email/SMS OTP, RFC 6238 TOTP (JDK-only), and app-keys
  (`TwoFactorService`, `/api/security/profile/**`).
- **Identity verification** — government documents (NIC / passport / driving licence) with
  admin review (`/api/identity/*`, `/api/admin/identity/{id}/review`).
- **PII at rest** — sensitive fields encrypted with AES-256/GCM (`PiiEncryptor`), with a
  masked-PII view (`/api/security/pii/me`).
- Tenant isolation on data; passwords hashed (BCrypt); secrets via env vars (never
  committed) — `JWT_SECRET`, `QR_SECRET`, `PII_MASTER_KEY`.
- Input validation on all DTOs; global exception handling.
- Fraud detection (0-100 score, flags, auto-block, audit signals) with admin override.

See `docs/security-privacy-guide.md` for the full security & privacy reference.

## 4. Deployment Topology

- **Build**: Maven; artifacts published to a registry.
- **Runtime**: containerized app + managed DB.
- **Scaling**: stateless app -> horizontal autoscaling (HPA/KEDA on EKS, ASG/ECS on AWS);
  DB scales vertically or via managed service.
- **Targets**: AWS, EKS, on-prem, Docker, single server — **plus Windows, Linux and any
  generic VPS/cloud** for self-hosting.
- **White-label**: any tenant/agent can run their own branded instance by configuration, or
  use a hosted managed service.

See `docs/deployment-autoscaling.md` for autoscaling, `docs/self-host-deployment.md` for
step-by-step self-hosting (Windows / Linux / Docker / K8s / cloud), and
`docs/white-label-guide.md` for per-tenant white-label deployment.
