# TicketMesh — Architecture Graphs

> Part 56 of the master blueprint. Textual (ASCII) architecture graphs covering context,
> containers, components, runtime and deployment. These complement `docs/architecture.md`
> (written narrative) and `docs/deployment-autoscaling.md` (operational).

## 1. System context (C4 Level 1)

```
                         ┌────────────────────────────┐
                         │   External consumers        │
                         └──────────────┬─────────────┘
                                        │ HTTPS/JSON
                                        ▼
   ┌────────────────────────────────────────────────────────────┐
   │                   TicketMesh Platform                       │
   │  +----------------------+   +----------------------------+  │
   │  | Consumer Web + PWA   |   | Admin / Agent / Ops        |  │
   │  | (React + TS)         |   | portals (React + TS)       |  │
   │  +----------+-----------+   +-------------+--------------+  │
   │             │     (branding, catalog, orders, search)      │
   │             ▼                                              │
   │  +------------------------------------------------------+  │
   │  |            ticketmesh-core  (Spring Boot 3.3.5)       |  │
   │  |  REST API + Security + domain services + AI/ML        |  │
   │  +----------------------+-------------------------------+  │
   │                         │                                  │
   │        +----------------+----------------+                 │
   │        ▼                                 ▼                 │
   │  +-----------+       +------------+   +-----------------+  │
   │  | MySQL/    |       | Provider   |   | Prometheus/     |  │
   │  | Postgres  |       | adapters   |   | Actuator health |  │
   │  +-----------+       +------------+   +-----------------+  │
   └────────────────────────────────────────────────────────────┘
```

## 2. Container view (C4 Level 2)

```
                              ┌─────────────────────────┐
                              │   Identity / Auth (JWT) │
                              │   Tenant & Branding     │
                              └──────────┬──────────────┘
   Consumer                 ┌────────────┼─────────────────────────┐
   Web/PWA       ─────────▶ │            ▼                         │
   ──────────────▶          │  ┌───────────────────────────────┐   │
   Agent/Shop ──────────▶   │  │      ticketmesh-core           │   │
   Admin/Admin ────────▶    │  │  - Catalog / Search            │   │
   Ops surface ────────▶    │  │  - Marketplace (shops/providers)│   │
                            │  │  - Booking / Payment / Refund  │   │
                            │  │  - Inventory / seat allocation │   │
                            │  │  - Ticket issuing / QR verify  │   │
                            │  │  - Pricing / Promotions / FX   │   │
                            │  │  - Loyalty / Reviews / Notifs  │   │
                            │  │  - Support + SLA escalation    │   │
                            │  │  - Fraud / risk scoring        │   │
                            │  │  - AI assistant / RAG / NL     │   │
                            │  │  - ML analytics (deterministic)│   │
                            │  │  - Outbox events / audit       │   │
                            │  │  - Ops / disruption            │   │
                            │  │  - 2FA/OTP/TOTP + app-keys     │   │
                            │  │  - Identity verify + PII/AES   │   │
                            │  │  - Messaging (WhatsApp/FB/TG/   │   │
                            │  │     SMS) omnichannel webhooks  │   │
                            │  │  - Social: family/settings/    │   │
                            │  │     referrals + vouchers/offers│   │
                            │  └──────────────┬────────────────┘   │
                            └─────────────────┼────────────────────┘
                                              ▼
                                 ┌───────────────────────┐
                                 │  MySQL / PostgreSQL   │
                                 │  + Flyway migrations  │
                                 └───────────────────────┘
```

Domains exposed to each channel (`docs/api-production.md` for full surface):

- **Consumer**: catalog search, product detail, pricing, orders, my orders, loyalty,
  notifications, trips, reviews, support, AI assistant; family groups, settings, referrals,
  vouchers/offers; multi-payment (CARD / WALLET / PAYPAL / BANK); 2FA + masked PII.
- **Agent**: shop apply / self-register with instant activation (`/api/agent/shops`),
  identity verification, connect provider (`/api/agent/providers`),
  publish/update/toggle products (`/api/agent/products*`), white-label branding, messaging
  channels.
- **Admin**: tenants (+ moderation mode, branding), identity review, providers status,
  fraud signals, messaging channels, promotions/vouchers/offers, audit, ops events,
  disruption.
- **Ops**: messaging/omnichannel monitoring, `/api/ops/events`, `/api/ops/disruption`,
  `/api/health/live`, `/actuator/health`.

## 3. Component view — domain modules (C4 Level 3)

```
                        ┌─────────────── Spring Core context ───────────────┐
                        │                                                   │
  REST boundary         │   Domain services (each own package + repo)       │
  ─────────────         │                                                   │
  AuthController ────▶  │  AuthService                  Tenant/Branding      │
  CatalogController ─▶  │  CatalogService  ◄── ProviderOfferClient (adapter) │
  AgentController ───▶  │  AgentOnboardingService  ── ProviderService        │
  OrderController ────▶ │  ProductOrderService ── PricingService ── Promo    │
  BookingController ─▶  │  ReservationService (pessimistic lock) ── Payment  │
  TicketController ──▶  │  TicketService (QR + HMAC)                         │
  SupportController ─▶  │  SupportService + SLA scheduler                    │
  FraudController ───▶  │  FraudDetectionService                             │
  AnalyticsController ─▶│  MlSuiteService (deterministic) + AiDataAnalyst    │
  AssistantController ─▶│  AiAssistantService + RAG + guardrails             │
  OpsController ──────▶ │  DisruptionService + EventService + AuditService   │
  SecurityProfileCtrl ─▶│  TwoFactorService (OTP / TOTP / app-key)           │
  IdentityController ─▶ │  IdentityVerificationService + PiiService          │
  MessagingController ─▶│  MessagingService + MessagingDispatcher            │
  FamilyController ───▶ │  FamilyGroupService                                │
  SettingsController ─▶ │  UserSettingsService                               │
  ReferralController ─▶ │  ReferralService  (+ LoyaltyService rewards)       │
  PaymentController ──▶ │  PaymentService + PaymentGatewayRegistry           │
                        │                                                   │
                        │   Cross-cutting: SecurityConfig, CurrentUser,     │
                        │   GlobalExceptionHandler, Outbox (Event)          │
                        └───────────────────────────────────────────────────┘
```

## 4. Runtime sequence — universal marketplace order

```
Customer                ticketmesh-core                     DB          Provider
   │  POST /api/orders     │                                │              │
   ├──────────────────────▶│ CatalogService.validate + lock  │              │
   │                       ├────────────────────────────────▶│ product      │
   │                       │ PricingService (base+tax+fee,  │              │
   │                       │   promo, FX)                   │              │
   │                       ├────────────────────────────────▶│ price/Loyalty│
   │                       │ ProductOrderService.save       │              │
   │                       ├────────────────────────────────▶│ order        │
   │                       │ decrement inventory (atomic)   │              │
   │                       ├────────────────────────────────▶│ inventory    │
   │                       │ apply loyalty + emit event     │              │
   │                       │   (outbox)                     │              │
   │                       └────────────────────────────────▶│ outbox       │
   │  orderRef + total ◀───┴─────────────────────────────────┘              │
   ├──────────────────────┤
```

## 5. Deployment / scaling topology

```
                     AWS / K8s / Docker
   ┌──────────────────────────────────────────────────────────┐
   │  LB / API edge ──▶ app replicas (stateless, autoscaled)   │
   │   /api/...   ──▶   ├─ pod/container A (ticketmesh-core)   │
   │                    ├─ pod/container B (ticketmesh-core)   │
   │                    └─ ... up to max (HPA / target-tracking)│
   │                                                           │
   │   Managed DB (MySQL/Postgres)  +  CloudWatch/Prometheus   │
   └──────────────────────────────────────────────────────────┘
```

Autoscaling details (min/max, metrics, probes, non-root) in
`docs/deployment-autoscaling.md`; Kubernetes manifests in `infra/kubernetes/`, Helm chart
in `infra/helm/ticketmesh/`, and AWS ECS Fargate Terraform in `infra/terraform/`.
