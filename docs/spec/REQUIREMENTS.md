# TicketMesh — Master Requirement Register (Single Source of Truth)

> This file is the authoritative, complete record of every requirement for the
> TicketMesh universal ticketing platform. It is the single source of truth and is
> kept in sync on every change. The final `.docx` / `.pdf` scope documents are
> rendered from this content by `tools/generate_docs.py`. It also drives the reusable
> "platform prompt" (`docs/PLATFORM_PROMPT_TEMPLATE.md`) used to build other products.

## 0. Product identity & positioning

- **Product name:** TicketMesh — Universal Configurable Ticketing & Reservation Platform.
- Brand tagline: **"All your tickets, one platform."**
- Marketed as a global, production-standard SaaS that can be: (a) bought as software,
  (b) self-hosted on the buyer's own server, or (c) run as a full managed service
  (hosting included) by the vendor.
- Target deploy targets: AWS, Kubernetes (EKS), on-premises, Docker, single server — with
  horizontal autoscaling and easy deployment everywhere.
- Prefer free / standard production SaaS-level tooling and self-hostable components
  (Docker, PostgreSQL/H2, Redis, Kafka, DuckDB, Prometheus, Grafana, local/offline AI).
- Sold as a business to any buyer: SaaS marketplace model, theme/white-label marketplace,
  managed-service offering, and on-prem licensing (WordPress-marketplace-like).

## 1. Core scope — the full 53-part master blueprint

The blueprint is not compressed here. Every one of the 53 parts is retained and tracked.
Honest per-part status lives in `docs/blueprint-coverage.md` (BUILT / PARTIAL / DOC / PLANNED).

### Part 1 — Product Vision & Scope
- Universal, configurable, multi-provider ticketing & reservation platform.
- Buy-as-software / self-host / managed-service with hosting.
- Sellable production standard; portfolio + visa evidence; startup/SaaS path.

### Part 2 — Global Benchmark & Product Positioning
- Benchmarked against global ticketing/reservation leaders and OTA aggregators.
- Differentiator: universal multi-domain aggregation + white-label SaaS + AI/ML + no-code.

### Part 3 — Universal Ticketing Model
- Core entities: `Tenant`, `TenantBranding`, `Provider`, `ProviderProduct` (ticket product),
  `TicketType`, `Inventory`, `Offer`, `Reservation`, `Booking`, `Ticket`, `Policy`,
  `Price`, `Fare`, `Addition/Fee`, `Tax`, `Promotion`, `Review`, `LoyaltyAccount`.

### Part 4 — Multi-Domain Support
- Domains: bus, train, movie, events, sports, flight, ferry, attractions, activities —
  each represented by configurable `TicketType` + capability + seat-layout semantics.

### Part 5 — Multi-Provider Aggregation
- Parallel search across connected providers; normalize, dedupe, compare, rank, sort.
- Universal `UniversalOffer` normalized view regardless of source provider.

### Part 6 — Provider Adapter Framework
- Adapter contract covering: search, availability, seat-map, price, hold, release, book,
  confirm, get-booking, cancel, refund, modify, ticket-retrieval, validation.
- `ProviderOfferClient` (RestClient) + WireMock stubs for offline dev.

### Part 7 — Provider Onboarding
- Provider registration + capability declaration + credentials + status (active/suspended).

### Part 8 — Capability System
- Providers declare supported capabilities; client routes only to capable providers.
- Capabilities: SEARCH, AVAILABILITY, SEAT_MAP, HOLD, BOOK, CANCEL, REFUND, MODIFY,
  TICKET_RETRIEVAL, VALIDATION, DYNAMIC_PRICING.

### Part 9 — Configuration-Driven Product Engine
- No-code/low-code: ticket products, ticket types, fields, seat layouts, policies, fees,
  taxes, promotions, notifications, validations — all configuration-driven via admin,
  no code changes required.

### Part 10 — Universal Search
- Unified search across all products/domains/providers with filters, sort, range, day.

### Part 11 — AI Conversational Search
- Natural-language booking intent: e.g. "bus from Colombo to Kandy tomorrow 6 pm".
- Offline-first intent parse + provider search + ranked offer response.

### Part 12 — Multi-Ticket / Multi-Service Trip
- A `Trip` bundles multiple bookings/tickets across domains into one itinerary.

### Part 13 — Smart Trip Planner
- Cross-domain journey planning (bus+train+event) with ordering and optimization hints.

### Part 14 — Inventory Management
- Inventory state machine: `AVAILABLE -> HELD -> CONFIRMED`; `HELD -> EXPIRED -> AVAILABLE`.
- Seat maps per product; per-tenant isolation; optimistic concurrency.

### Part 15 — Reservation & Concurrency
- Idempotency keys, optimistic locking, hold expiry, reservation release scheduler,
  reconciliation — prevent double-selling at scale.

### Part 16 — Booking Engine
- Universal booking flow: offer -> hold -> payment -> confirm -> issue ticket.
- Bookings across all domains through one engine.

### Part 17 — Payment Abstraction
- Payment provider abstraction: cards, wallets, bank, refunds, webhooks.
- Pluggable payment gateway adapters (WireMock-stubbed in dev); async status poll/webhook.

### Part 18 — Refund & Cancellation
- Cancellation policies per product/ticket-type; refund calculation; partial refunds.

### Part 19 — Ticket Issuing & Validation
- Issue QR/barcode tickets deterministically (ZXing); `/api/tickets/{id}/validate` for
  gate validation; secure, tamper-evident payload.

### Part 20 — Pricing & Fare Engine
- Base price + configurable fare components: fees, taxes, service fees, currency;
  price as `CurrencyAmount` (atomic currency + amount); multi-currency aware.

### Part 21 — Promotion Engine
- Coupons and promo rules (percentage / flat / min-purchase), scoped to tenant/domain;
  applied at checkout, validated, single use per booking.

### Part 22 — Customer & Loyalty
- Loyalty accounts per customer; earn points on bookings; levels; redeemable.
- Customer self-serve + agent-facilitated purchase both supported.

### Part 23 — Admin & Operations Console
- Admin portal: manage tenants, providers, agents, shops, products, monitor system,
  view analytics, audit, support, security ops.

### Part 24 — No-Code / Low-Code Configuration
- Admin UI + API to configure themes, products, fees, policies — no code.

### Part 25 — Multi-Tenancy SaaS
- Tenant isolation (data scoping by tenant), per-tenant white-label branding, per-tenant
  configuration, per-tenant currency/language/locale, subscription status.

### Part 26 — AI Assistant
- Conversational assistant (offline-first deterministic model; adapter for LLM later).

### Part 27 — AI Agent Architecture
- Controlled tools, least privilege, financial confirmation, audit trail of tool calls.

### Part 28 — AI Recommendation Engine
- Product/service recommendations from history + popularity (offline deterministic + ML).

### Part 29 — AI Seat Recommendation
- Suggest seats from preference/layout heuristics (offline).

### Part 30 — AI Price Prediction
- Predict future price moves from trend/heuristic model (offline + ML).

### Part 31 — Dynamic Pricing Intelligence
- Configurable fare rules / surge heuristics applied at pricing time.

### Part 32 — AI Fraud & Cyber Risk
- Fraud scoring 0-100, signals, auto-block >= threshold, admin override, audit.
- Cyber-risk awareness integrated with security ops.

### Part 33 — Disruption & Recovery Intelligence
- Detect disruption indicators (route/service changes) and propose recovery options.

### Part 34 — Ticket Intelligence / Data Analytics
- Trend analytics: bookings, revenue, demand, top products/domains, retention.

### Part 35 — AI Data Analyst
- Natural-language questions over a read-only semantic warehouse layer (offline).

### Part 36 — Data Platform
- Event-capture (outbox/events), analytics warehouse, Parquet/DuckDB offline, S3 option.

### Part 37 — ML Platform
- Demand forecasting, cancellation prediction, fraud scoring, recommendation, anomaly.
- Pure-Java/Python deterministic implementations (no heavy runtime dependency) + pluggable.

### Part 38 — Event-Driven Architecture
- Outbox-style event emission on domain events; async consumers/scheduler.

### Part 39 — Microservices Architecture
- Modular single-core delivered this build; microservice-ready boundaries (identity,
  catalog, provider, search, inventory, reservation, booking, payment, ticket,
  notification, fraud, analytics). Documented splitting path.

### Part 40 — API Gateway & Edge
- Central entry-point pattern, auth filter, tenant resolution, rate limiting, routing
  contract. (In-service edge layer; dedicated gateway optional.)

### Part 41 — Security Architecture
- OAuth2/JWT, RBAC (CUSTOMER/AGENT/ADMIN), tenant isolation, field-level redaction,
  audit log, rate limiting, input validation, encryption, PII minimization, secrets via
  env, security headers, CSRF-stateless API.

### Part 42 — AI Security
- Prompt-injection defense, PII redaction, tool authorization, output validation,
  retrieval filtering, model allowlist, audit, adversarial eval.

### Part 43 — DevOps / DevSecOps
- CI, security pipeline (Gitleaks/OWASP/Checkov), release automation, IaC.

### Part 44 — CI/CD Pipeline
- GitHub Actions CI (Java test + frontend build) on push/PR; security scans; release on tag.

### Part 45 — Kubernetes & GitOps
- K8s manifests + Helm chart + HPA autoscaling + optional Argo Rollouts.

### Part 46 — AWS Cloud Architecture
- Terraform: ECS Fargate (or EKS), ALB, RDS, scaling policies, CloudWatch, IAM.

### Part 47 — Free / Offline Development
- Offline AI model (deterministic), H2 in-memory DB for tests, WireMock provider/payment
  stubs — start MVP without paid APIs.

### Part 48 — Frontend Web & Mobile Strategy
- Consumer Web (React + TypeScript PWA installable = web + app channel),
  Agent shop portal, Admin portal. Responsive + PWA offline shell.

### Part 49 — Testing & Quality
- Unit tests (real assertions), integration tests, WireMock integration tests,
  fraud/support/AI tests. Documented test strategy.

### Part 50 — Observability
- Health endpoints, actuator metrics, structured logs, request tracing id, monitoring
  docs + prometheus/grafana config.

### Part 51 — Repository / Git Format
- Monorepo layout: `src` (backend modules), `frontend`, `docs`, `infra`, `tools`, CI.

### Part 52 — Project Delivery Roadmap
- Phased roadmap from MVP to production SaaS.

### Part 53 — MVP + Definition of Done + Portfolio Evidence
- MVP scope, Definition of Done, and portfolio/evidence artifacts.

## 2. Additional requirements given during the build session (all MUST be in docs)

### 2.1 Marketplace / agent model (Uber / PickMe style)
- **Customer / Agent / Admin** roles all exist.
- Any **shop / agent** can connect its service and sell to customers.
- A customer can also use the platform directly by themselves (self-serve).
- Agents/shops connect **via app**, upload their **ticket details and services**, publish
  products to sell tickets to customers.
- **Shop owner can build/manage their shop using just their app**; admin manages overall
  and can **monitor** everything.
- Shop self-service: publish product + inventory + seat info + price from the agent app.

### 2.2 Global / white-label / themeable (WordPress / Uber / PickMe style)
- Standard **global product usable from any country, any currency, any language**.
- **Everything configurable** like WordPress themes or Uber/PickMe: theme colors, images,
  **logo**, typography, and anything else the shop needs — purely via configuration (no code).
- **Multi-currency** first-class (`CurrencyAmount` = currency code + amount; per-tenant
  default currency; configurable exchange).
- **Multi-language / i18n** first-class (per-tenant default locale + region/country;
  translation dictionaries; client-selectable language).
- **Country / region** scoping (per-tenant country, currency, locale).
- Multi-tenant SaaS white-label branding.

### 2.3 Channels
- **Web** channel (consumer web) and **app** channel (mobile-responsive PWA installable).
- **Agent shop portal** for shop owners to manage their shop & publish products.
- **Admin portal** for overall management + monitoring.

### 2.4 Deployment & operations (production SaaS)
- Deployable to **AWS, Kubernetes, on-prem, Docker** — horizontal **autoscaling**, easy deploy.
- Buy-as-software, self-host, or **full managed service with hosting**.
- Mostly **free + standard** production SaaS-level tooling.

### 2.5 Testing / API tooling
- Use **WireMock** for offline provider/payment stubbing where anything cannot connect.
- Provide **test** API documentation and **production** API documentation.

### 2.6 Support & 24/7 operations
- **Support service** and a **24/7 support portal with management**.
- Include all **e-services** needed for a production application.
- Development **release notes**, **QA release notes**, and every document needed
  start-to-end: architecture diagrams, manuals, **SDLC process**, as a standard company.

### 2.7 Automation / proactive capabilities
- **Auto-detect issues** (live + dev + QA).
- **Fraud detection system**.
- **Auto-fix** live, dev and QA issues where feasible.
- **Full automation with a support system**.
- Monitoring, health checks, observability.

## 3. Product-name & business direction guidance

- Blueprint product name: **TicketMesh** — "All your tickets, one platform."
- Multi-product company portfolio later (e-commerce, hotel, e-channel, fintech, daycare,
  learning, care, portals, apps) — all reuse the same **platform prompt + architecture +
  docs**. Ticketing is built first; each product mirrors it.
- Sell through multiple channels like the WordPress marketplace model.

## 3.5 Delivery decision & blueprint coverage

- **Delivery decision:** the blueprint's multi-microservice topology is delivered this build
  as a **pragmatic modular single-backend** (`ticketmesh-core` Spring Boot) with clear
  within-core module boundaries (identity, catalog, provider, search, inventory,
  reservation, booking, payment, ticket, notification, fraud, analytics, AI/ML). This
  stays coherent and shippable while remaining microservice-ready.
- **Coverage mapping:** see `docs/blueprint-coverage.md` for the honest per-part status.

## 4. Non-negotiable (from CLAUDE.md — authorship/quality)

- Author: **sangiya** (asan935para@gmail.com). No "Claude"/"AI-generated" anywhere.
- Production-grade only; real tests with real assertions; professional repo names.
- Data safety: never commit real production data, credentials, or customer info.
- No employer proprietary content.
