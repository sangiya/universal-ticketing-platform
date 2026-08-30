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
