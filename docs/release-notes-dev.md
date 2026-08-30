# TicketMesh — Development Release Notes

> Release notes for the engineering releases. Version 1.0.0 is the first production
> development release of the TicketMesh universal ticketing platform.

## v1.0.0 — Development Release (2026-08-30)

### Highlights
- Universal, multi-provider ticketing/reservation core (bus, train, movie, events, sports,
  flight, ferry, attractions).
- Marketplace (Uber/PickMe-style): customer / agent / admin roles; shops apply via app,
  upload and sell their own products; admins monitor everything.
- Global white-label SaaS: country (ISO 3166-1), currency (ISO 4217), language (BCP-47),
  timezone and per-tenant theming (colors, logo, images) — all by configuration.
- Channels: React consumer Web + mobile-responsive PWA, plus admin portal.

### New features
- Identity & auth: register/login, JWT, roles `CUSTOMER` / `AGENT` / `ADMIN`, tenant
  resolution, bootstrap admin seed.
- Tenant & branding service: create tenants, upload/store white-label branding.
- Agent onboarding: shop application -> approval workflow.
- Provider & catalog: provider connect, product upload, universal catalog search.
- Booking/reservation/payment: seat allocation, reservation hold/expiry, payment, refund,
  QR ticket issue & validation.
- AI assistant + RAG retrieval + guardrails.
- Support service: 24/7 tickets (open/reply/assign/escalate/SLA), automated SLA breach
  escalation.
- Fraud detection: 0-100 risk scoring, named flags, auto-block, persisted signals, admin
  override; high-risk counter and signals API.
- Ops/health: liveness + readiness + actuator health/readiness/metrics; management
  endpoints.

### Fixes in this release
- Renamed legacy package/artifact to the TicketMesh brand and booking reference prefix.
- Restored content for models/controllers that were truncated during refactor.
- Corrected request DTOs to expose setters for constructors/integration use.
- H2 MySQL-mode + Flyway migrations for cross-environment consistency.

### Testing
- 62 automated tests passing (unit + integration + WireMock contract).
- Coverage across booking, payment, ticket, support, fraud, AI, marketplace and full-flow.

### Known limitations
- Payment uses an abstraction over a stubbed/offline provider (WireMock) — production
  gateway wiring is configured by deployment.

### Up next (development)
- React consumer PWA + admin portal wiring to these APIs.
- Full deployment/autoscaling infrastructure and production API documentation finalization.

## v1.1.0 — Development Release (2026-08-30)

### Highlights
- Marketplace commerce: **universal product orders** (checkout via `/api/orders`),
  promotions, loyalty points/tiers, reviews, notifications, multi-leg trips and live pricing.
- Globalization: per-locale translation dictionaries and multi-currency FX rates.
- ML / analytics: deterministic ML-style suite (demand forecast, price prediction,
  recommendations, trend report, anomaly scoring) plus a natural-language data analyst.

### New features
- **Orders** — universal marketplace checkout `ProductOrderController` + `ProductOrderService`
  (`POST /api/orders`): inventory validation + decrement, transparent price breakdown,
  promo redemption, loyalty earning, notification dispatch and outbox event emission.
- **Promotions** — create/enable/disable promo codes (discount type, value, min purchase,
  validity window, max uses, domain scoping) via `/api/promotions`; applied in pricing.
- **Loyalty** — `LoyaltyAccount` with points and tier via `/api/loyalty`.
- **Reviews** — create + list + average rating per product via `/api/reviews`.
- **Notifications** — current-user notification feed via `/api/notifications`.
- **Trips** — multi-ticket/multi-service trip builder (create trip, add legs) via `/api/trips`.
- **Pricing** — final price with promo code + currency conversion via `/api/pricing/{productId}`.
- **Globalization** — translation dictionary + FX rates + currencies + languages via
  `/api/globalization` (multi-currency / multi-language SaaS).
- **ML / analytics** — `com.ticketmesh.ml.MlSuiteService` (demandForecast,
  cancellationProbability, recommend, predictPrice, anomalyScore, trendReport) and
  `AiDataAnalystService` (NL question answering), exposed via `/api/analytics`.
- **Disruption & audit** — `DisruptionService` -> `DisruptionReport` (active disruptions,
  findings, recovery recommendations) via `/api/ops`, plus outbox event feed and audit log
  via `/api/ops/events` and `/api/audit`.

### Testing
- Expanded test suite from **62 to 82** passing tests (unit + integration + WireMock
  contract + full flow + marketplace order flow).
- New coverage: analytics/ML, promotions, loyalty, reviews, notifications, trips,
  globalization, pricing and the universal order lifecycle.

### Known limitations
- ML outputs are deterministic heuristics over booking/order history (no trained model);
  suitable as explainable baselines for dynamic pricing and demand intelligence.
- Payment uses an abstraction over a stubbed/offline provider (WireMock) — production
  gateway wiring is configured by deployment.

## v1.2.0 — Development Release (2026-08-31)

### Highlights
- **Open, instant-activation marketplace** with configurable **moderation mode**
  (`INSTANT` / `REVIEW`) per tenant (`PUT /api/admin/tenants/{slug}/moderation?mode=`).
- **Strong onboarding security**: 2FA (OTP / TOTP / app-keys), identity verification with
  admin review, and **AES-256/GCM PII encryption** with a masked-PII view.
- **Omnichannel messaging**: WhatsApp / Facebook / Telegram / SMS webhooks, outbound send,
  and per-tenant channel integrations.
- **Multiple payment methods**: CARD, WALLET, PAYPAL, BANK via a pluggable gateway
  abstraction.
- **Social & commerce**: family groups, per-user settings, referrals / invite friends
  (+ loyalty rewards), and **VOUCHER / OFFER** promotion kinds.

### New features
- **2FA / security profile** — `/api/security/profile/**`: email/SMS OTP, RFC 6238 TOTP
  (JDK-only HMAC-SHA1, 30s window), and recoverable app-keys (`TwoFactorService`).
- **Identity verification** — submit NIC / passport / driving licence + photo encrypted,
  with admin review at `/api/identity/*` and `/api/admin/identity/{id}/review`.
- **PII security** — `PiiEncryptor` (AES-256/GCM, per-record IV) + masked-PII endpoint
  `GET /api/security/pii/me`; `PiiService`, `Base32`, `pii::master-key` env.
- **Omnichannel messaging** — `MessagingController`/`MessagingService`,
  `MessagingDispatcher` + `WhatsAppMessagingAdapter`; public webhook ingestion
  (deduplicated by external ref), outbound replies, conversation log and channel config.
- **Multi-payment gateways** — `PaymentGatewayRegistry` with `CardGateway`,
  `WalletGateway`, `PayPalGateway` and a lenient `OfflineGatewayStub` (BANK/UPI);
  honored by `PaymentService` + `/api/payments/**`.
- **Family groups** — create / join / remove-member / list (+ members) via `/api/family`.
- **Per-user settings** — theme / language / currency / notification preferences via
  `GET`/`PUT /api/settings`.
- **Referrals** — generate codes, invite by email (encrypted), validate publicly, reward
  100 loyalty points to the referrer on join, via `/api/referrals*`.
- **Vouchers & offers** — promotion `kind` extended to `VOUCHER` / `OFFER` alongside
  `PERCENT` / `FLAT` discount types.
- **Open registration** — `AgentOnboardingService` auto-approves a shop when the tenant's
  moderation mode is `INSTANT`, auto-provisioning tenant + provider for instant selling.

### Fixes in this release
- PII (identity document numbers, contact copies, invitee emails) now encrypted at rest;
  referral invitee emails are masked in responses.
- Payment requests expose the payment method explicitly; gateway registry routes to the
  correct offline backend per method (CARD / WALLET / PAYPAL / BANK).

### Testing
- Expanded test suite from **82 to 135** passing tests (0 failures, BUILD SUCCESS) — unit +
  integration + WireMock contract + full flow.
- New coverage: TwoFactorService, IdentityVerificationService, PiiEncryptor, MessagingService,
  FamilyGroupService, UserSettingsService, ReferralService, and expanded promotion/payment/
  auth tests.

### Known limitations
- Messaging/WhatsApp adapters default to deterministic offline stubs; real transport is
  enabled by `WHATSAPP_ENABLED` / `WHATSAPP_ENDPOINT`. Payment gateways are offline/
  deterministic stubs; production processors are wired by deployment configuration.
