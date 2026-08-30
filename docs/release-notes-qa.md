# TicketMesh — QA Release Notes

> Quality assurance release notes. Documents what was verified for the v1.0.0 production
> readiness release.

## v1.0.0 — QA Release (2026-08-30)

### Test summary
- **Total automated tests:** 62
- **Failed:** 0
- **Pass rate:** 100%

### Coverage by area
| Area | Tests | Notes |
|------|-------|-------|
| Booking / reservation | 5 | seat allocation, full book, travel-date mismatch, cancel + refund |
| Ticket issue / validation | 5 | QR issue + verify paths |
| Payment | 7 | pay, refund, webhook/state |
| Support service | 7 | open, priority SLA, assign, escalate, queue, SLA auto-escalation |
| Fraud detection | 4 | low/medium/high, admin override, persist signals |
| AI assistant / RAG / guardrails | 15 | assistant, retrieval, guardrail enforcement |
| Marketplace full flow | 7 | admin tenant+branding, agent shop apply->approve, provider connect, upload, search, dashboard |
| Provider (WireMock contract) | 2 | offline provider stub real responses |
| JWT service | 3 | signing/validation/expiry |
| End-to-end full flow | 7 | register->book->pay->issue->verify |

### Test types executed
- **Unit tests** — service-level, real assertions, mocked boundaries.
- **Integration tests** — H2 MySQL-mode + MockMvc, full request/response flows.
- **Contract tests** — WireMock standalone for external provider API (works offline).

### Known issues / notes
- External provider and payment endpoints are stubbed with WireMock for offline dev; the
  real adapters are wired at deployment time via configuration.
- Actuator management endpoints exposed for the ops surface (health/readiness/metrics).

### Go / no-go
- **Result:** GO for production deployment baseline with documented provider/payment wiring.

### Regression risk
- Low; the release adds domains (support, fraud, ops) without changing existing booking,
  payment, ticket, AI or marketplace behaviour. All prior tests remain green.

## v1.1.0 — QA Release (2026-08-30)

### Test summary
- **Total automated tests:** 82
- **Failed:** 0
- **Pass rate:** 100%

### What was added since v1.0.0
- Marketplace commerce: **universal product orders** (`POST /api/orders`), promotions,
  loyalty, reviews, notifications, multi-leg trips, live pricing.
- Globalization: translation dictionaries + multi-currency FX rates (multi-language /
  multi-currency SaaS).
- ML / analytics: `MlSuiteService` (forecast, price, recommend, anomaly, trend) +
  `AiDataAnalystService` NL data analyst, exposed via `/api/analytics`.
- Ops intelligence: `DisruptionService` -> `DisruptionReport`, outbox event feed and audit.

### Coverage by area (added in v1.1.0)
| Area | Notes |
|------|-------|
| Orders | checkout via `ProductOrderService`/`ProductOrderController`: inventory check + decrement, pricing breakdown, promo, loyalty, notification, outbox event |
| Analytics / ML | demand forecast, price projection, recommend, trend, anomaly, NL analyst |
| Promotions | create, list, toggle, apply in pricing |
| Loyalty | points / tier account |
| Reviews | create, list, average rating |
| Notifications | current-user feed |
| Trips | create trip, add legs, list |
| Globalization | translate, dictionary, rates, currencies, languages |
| Pricing | promo + currency final price |
| Ops / audit | events feed, disruption report, audit log |

### Test types executed
- **Unit tests** — service-level, real assertions, mocked boundaries.
- **Integration tests** — H2 MySQL-mode + MockMvc, full request/response flows.
- **Contract tests** — WireMock standalone for external provider API (works offline).

### Known issues / notes
- ML outputs are deterministic heuristics over history (explainable baselines); no trained
  model dependency.
- External provider and payment endpoints are stubbed with WireMock for offline dev; the
  real adapters are wired at deployment time via configuration.

### Go / no-go
- **Result:** GO for production deployment baseline with documented provider/payment wiring.

### Regression risk
- Low; v1.1.0 adds commerce, globalization and ML/analytics domains without changing
  existing booking, payment, ticket, support, fraud, AI or marketplace behaviour. All prior
  tests remain green (62 -> 82 total).

## v1.2.0 — QA Release (2026-08-31)

### Test summary
- **Total automated tests:** 135
- **Failed:** 0
- **Pass rate:** 100%
- **Build:** BUILD SUCCESS

### What was added since v1.1.0
- Open, instant-activation marketplace with per-tenant **moderation mode** (`INSTANT` /
  `REVIEW`).
- Strong onboarding security: **2FA** (OTP / TOTP RFC 6238 / app-keys), **identity
  verification** + admin review, **AES-256/GCM PII encryption** + masked-PII endpoint.
- **Omnichannel messaging**: WhatsApp / Facebook / Telegram / SMS webhooks + outbound send.
- **Multi-payment**: CARD / WALLET / PAYPAL / BANK via pluggable gateway abstraction.
- **Social & commerce**: family groups, per-user settings, referrals / invite friends,
  and VOUCHER / OFFER promotion kinds.

### Coverage by area (added in v1.2.0)
| Area | Notes |
|------|-------|
| 2FA / security profile | OTP send+verify, TOTP enable+verify (RFC 6238, JDK-only), app-key issue+verify |
| Identity verification | submit (NIC / passport / driving licence + photo), status, admin review |
| PII encryption | `PiiService`/`PiiEncryptor` AES-256/GCM round-trip, masked-PII view |
| Omnichannel messaging | public webhook (dedup by external ref), outbound reply, conversation log, channel config |
| Payments (gateways) | CARD / WALLET / PAYPAL / BANK routing and settle/status |
| Family groups | create, join, remove member, list, members |
| Settings | read/update theme, language, currency, notification prefs |
| Referrals | generate code, invite (email encrypted), validate (public), reward points on join |
| Promotions | VOUCHER / OFFER kinds alongside PERCENT / FLAT; apply in pricing |
| Open registration | moderation mode `INSTANT` auto-approves shop + tenant/provider auto-provision |

### Test types executed
- **Unit tests** — service-level, real assertions, mocked boundaries.
- **Integration tests** — H2 MySQL-mode + MockMvc, full request/response flows.
- **Contract tests** — WireMock standalone for external provider API (works offline).

### Known issues / notes
- Messaging/WhatsApp adapters default to deterministic offline stubs; real transport is
  enabled by `WHATSAPP_ENABLED` / `WHATSAPP_ENDPOINT`. Payment gateways are
  offline/deterministic stubs; production processors are wired by deployment configuration.
- Referral rewards are loyalty points (100 per successful join) — deterministic and idempotent.

### Go / no-go
- **Result:** GO for production deployment baseline with documented provider/payment wiring.

### Regression risk
- Low; v1.2.0 adds security, social, messaging and gateway surfaces without changing
  existing booking, payment, ticket, support, fraud, AI, commerce or marketplace behaviour.
  All prior tests remain green (82 -> 135 total).
