# TicketMesh — Global Benchmark & Positioning

> Part 02 of the master blueprint. Honest competitive positioning of TicketMesh against
> the leading universal ticketing / reservation platforms, and where it wins.

## 1. Competitive landscape

| Platform | Model | Core domains | Strengths | Gaps / where TicketMesh differs |
|----------|-------|--------------|-----------|----------------------------------|
| **Ticketmaster** | Centralized ticketing | Events, sports, live | Huge venue/event network, scale | Closed marketplace; no bus/train/attractions; no white-label per-tenant SaaS; limited self-service merchant onboarding |
| **BookMyShow** | Centralized + mall kiosk | Movies, events, sports | Strong movie first-mover, SMS/QR ticketing | India-centric; not a universal configurable multi-tenant SaaS |
| **Uber / PickMe** | Marketplace (demand/supply matching) | Mobility | Two-sided marketplace flywheel, driver/agent onboarding, real-time ops | Mobility-only; not ticketing; no per-tenant white-label |
| **Busbud / Trainline / Wanderu** | Aggregator | Bus / train | Strong multi-provider aggregation and search | Aggregation only (no issuing/payments for all providers); not configurable SaaS |
| **Sabre / Amadeus** | Travel GDS | Flight, rail | Huge legacy network, PNR ecosystem | Closed, expensive, not a per-tenant SaaS product businesses white-label |
| **Eventbrite** | SaaS event platform | Events | Self-service event creation | Events-centric; no multi-domain universal model, no QR-gate/ops controls or marketplace roles |

## 2. TicketMesh positioning

TicketMesh is a **universal, configurable, multi-tenant ticketing & reservation SaaS**
that unifies the strengths above into a single platform:

- **Universal domain model** — bus, train, movie, events, sports, flight, ferry,
  attractions under one product/booking model (not one vertical per codebase).
- **Marketplace roles** (Uber/PickMe-style) — `CUSTOMER`, `AGENT` (merchant/shop),
  `ADMIN`; agents/shops self-onboard, connect a provider, publish and sell their own
  services; customers self-serve; admins monitor everything.
- **White-label multi-tenant SaaS** — per-tenant branding, language, currency, timezone
  configured per tenant, rendered without code.
- **Provider aggregation + adapters** — canonical provider client and WireMock-stubbed
  contracts make adding a provider cheap and offline-testable.
- **Open marketplace + instant activation** — any agent self-registers and starts selling
  immediately (`INSTANT` moderation) or under admin review (`REVIEW`), per tenant.
- **Strong KYC & 2FA** — identity verification (NIC / passport / driving licence) with admin
  review, OTP/TOTP/app-key 2FA, and PII encrypted at rest (AES-256/GCM) with masked-PII view.
- **Omnichannel** — WhatsApp / Facebook / Telegram / SMS messaging with public webhooks and
  outbound send.
- **Full vertical ops** — booking, payment (CARD / WALLET / PAYPAL / BANK), refund,
  inventory/seat allocation, QR issuing & gate validation, 24/7 support with SLA,
  fraud/risk scoring.
- **AI + deterministic analytics** — conversational assistant + RAG, NL data analyst,
  demand forecast, price prediction, recommendations, anomaly scoring — all optional and
  layered, without forcing an LLM into the money path.

## 3. Where TicketMesh wins vs. the incumbents

1. **Configurability** — incumbents are either closed (Ticketmaster) or single-domain
   (BookMyShow movies, Uber mobility). TicketMesh is config-driven across domains and grows
   a new vertical by configuration rather than a new product.
2. **White-label per-tenant SaaS** — a bus operator, event promoter, or cinema chain can
   each run their own branded storefront from the same core with zero code; incumbents
   force their own branded funnel.
3. **Agent self-service marketplace** — merchants onboard, publish and price their own
   catalog (Uber/PickMe model) while still enjoying the aggregated consumer reach and the
   QR/ops tools.
4. **Offline-friendly + deterministic AI** — full offline development (H2, WireMock,
   deterministic payment simulator) and deterministic ML-style analytics mean the money
   path never depends on a live third-party API, unlike pure-LLM competitors.
5. **Total cost / self-host** — runs on Docker, K8s/EKS, or AWS Fargate with autoscaling;
   can be sold as licensed software, self-hosted, or managed — more delivery flexibility
   than the closed SaaS incumbents.
6. **Open activation + strong trust** — an agent can go live and start selling instantly
   (Uber/PickMe model) while the platform still enforces strong identity/KYC and 2FA and
   encrypts PII — the same combination the incumbents lack: instant self-service supply
   growth *and* a trustworthy, secure buyer experience.

## 4. Honest gaps to close for direct head-to-head

| Area | Incumbent advantage | TicketMesh path |
|------|---------------------|-----------------|
| Venue/network scale | Ticketmaster's live venue/inventory | Add more seeded providers + adapters; marketplace grows supply |
| Mobility real-time ops | Uber's live driver/positioning | Smart trip planner + disruption recovery already modeled; add geo/ETA adapters |
| Legacy GDS reach | Sabre/Amadeus PNR network | Implement a Sabre-style provider adapter against the canonical client |
| Movie ex-first mover | BookMyShow's regional network | Multi-domain universal model already covers movies; add regional providers |

The modular single-backend design means none of these are rewrites — each is adding an
adapter or a within-core module behind the existing surfaces.
