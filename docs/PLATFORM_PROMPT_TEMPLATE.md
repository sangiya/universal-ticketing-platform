# Reusable Platform Prompt Template

> Build a full production-grade SaaS platform once, then reuse this prompt + architecture +
> docs pattern for every new product (e-commerce, e-hotel management, e-channel, fintech /
> lending, daycare, learning, elder-care / child-care, portals, apps, portfolios). The
> TicketMesh platform was the first build produced from this exact template.

---

## How to use this template

1. Copy this file to the new product repo.
2. Replace the bracketed placeholders: `[PRODUCT NAME]`, `[MODEL / ENTITY]`, `[CHANNELS]`,
   `[TECH STACK]`, `[REPO]`.
3. Answer the two decision questions below (they drive the whole build).
4. Generate the master scope, the code, the tests, the infra, and the full document set by
   working through the section checklist left-to-right — do not skip.
5. When every checklist item is done, run the final gate and push to GitHub.

---

## 0. Decision questions (answer before building)

- **Q1 — App channel:** Which channel(s) must the consumer use? (Recommended: React web +
  mobile-responsive **PWA** app-installable.)
- **Q2 — Delivery scope:** (a) one modular backend + one PWA, (b) per-product microservices,
  or (c) a platform of isolated services? (Recommended for speed + coherence: **one modular
  backend** exposing clear domains, deployed as a service.)
- **Q3 — Repo name:** professional, non-"lab"/"demo" name.

## 1. Mandatory product pillars

- **Roles:** `CUSTOMER`, `AGENT`, `ADMIN` — plus any domain-specific roles ([PRODUCT]).
- **Marketplace / agent model** (Uber / PickMe style): agents/shops connect via app, upload
  their own products/services, sell to customers; a customer can also self-serve directly;
  shop owners manage their own shop from the app; admins monitor everything.
- **Global / white-label / themeable** (WordPress / Uber / PickMe style): usable from any
  country (ISO 3166-1 alpha-2), any currency (ISO 4217), any language (BCP-47). Everything
  configurable — theme, colors, images, logo — via configuration, no code. Multi-tenant
  white-label branding per tenant.
- **Channels:** consumer Web + app (PWA), plus an admin portal for management & monitoring.
- **Monetization paths:** buy-as-software, self-host on buyer's server, or full managed
  service with hosting included.

## 2. Core functional model

Define the universal domain model for `[MODEL / ENTITY]`. For ticketing this was:
`Product / Type / Provider / Inventory / Offer / Reservation / Booking / Ticket / Policy`.
Map the template to your domain:

| Template concept        | Your domain                                                              |
|-------------------------|--------------------------------------------------------------------------|
| Product                 | the sellable item (ticket, room, course, slot, policy, plan)             |
| Provider                | the shop/agent/service supplying it                                      |
| Inventory               | available -> held -> confirmed, expiry + reconciliation                  |
| Offer / pricing         | fare/price, taxes, fees, promotions, dynamic pricing                     |
| Reservation             | hold with concurrency + idempotency keys + optimistic locking            |
| Booking / Order         | confirmed purchase, cancellation, refund                                 |
| Delivery / Fulfilment   | ticket/QR/barcode issue, coupon, enrolment, scheduling                   |
| Policy                  | configurable per-tenant business rules                                   |

## 3. Cross-cutting must-haves (all of these, always)

- **AI assistant** + AI agent architecture (controlled tools, least privilege, financial
  confirmation, audit trail).
- **Recommendation / price prediction / dynamic pricing**.
- **Fraud detection** + cyber-risk scoring (0-100 score, named flags, auto-block, persist
  signals, admin override).
- **Auto-detect issues** (live + dev + QA) and **auto-fix** where remediation is scriptable.
- **Monitoring, health checks (liveness/readiness), observability, metrics**.
- **24/7 support** service + support portal (tickets, SLA, assignment, escalation, queue).
- **All e-services for a production app**: auth, RBAC/ABAC, tenant isolation, encryption,
  secrets, audit, rate limiting, notifications (email/SMS/push), payments + refunds.
- **Testing**: real tests with real assertions; **WireMock** for offline stubbing of any
  external service that cannot connect immediately.
- **API docs**: separate **test** and **production** documentation sets.

## 4. Documentation set (produce start-to-end, as a standard company)

1. Master requirement register (single source of truth).
2. Scope document (`.docx` **and** `.pdf`).
3. Architecture diagrams (system context, container, deployment).
4. Development **release notes**.
5. QA **release notes**.
6. **SDLC process** document (planning -> build -> test -> release -> operate).
7. User manuals (consumer, agent/shop owner, admin).
8. Operations / runbook manual.
9. API reference — test env and prod env.
10. DevOps / deployment / autoscaling guide.

## 5. Delivery roadmap & DoD

- **MVP** first: core model + booking/order + auth + admin + PWA + tests.
- Then: marketplace, AI, fraud, support portal, automation.
- Then: full docs, infra, autoscaling, CI/CD, test + prod API docs.
- **Definition of Done:** build passes; all tests green; docs complete; infra present;
  lint/typecheck clean; pushed to GitHub by the authorised author.

## 6. Reuse notes

- Keep the **same architecture** (modular backend, PWA front, infra) so skills transfer.
- Keep the **same document templates** so each product ships with a complete start-to-end
  package.
- Keep **multi-channel external integration** (WhatsApp / LinkedIn / Facebook) in the design
  surface so products can plug into chat + social channels.
- Author every commit as the owner (no "Claude"/"AI-generated" branding).

---

*Template version 1.0 — first used to build TicketMesh (`universal-ticketing-platform`).*
