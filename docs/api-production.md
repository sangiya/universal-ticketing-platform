# TicketMesh — API Reference (PRODUCTION)

> Production API documentation for TicketMesh v1.3.0. This is the stable public contract.
> All requests and responses are JSON. Authentication uses OAuth2/JWT Bearer tokens.

## Base URL (production)

```
https://api.ticketmesh.example/api
```

Replace with the tenant-specific or region endpoint provided on onboarding. For a
multi-tenant deployment, API endpoints are tenant-scoped by path (`/tenant/{slug}`) or via
the authenticated tenant of the token.

## Security model

- **Authentication**: obtain a JWT via `POST /api/auth/login` (customer, agent or admin).
- **Authorization**: roles `CUSTOMER`, `AGENT`, `ADMIN` at method level.
  - `/api/support/tickets`, `/api/tickets`, `/api/catalog` — customers/agents (authenticated).
  - `/api/analytics/**`, `/api/pricing/**`, `/api/promotions`, `/api/loyalty`, `/api/reviews`,
    `/api/notifications`, `/api/trips`, `/api/globalization/**` — authenticated.
  - `/api/ops/**`, `/api/audit`, `/api/admin/**` — `ADMIN` or `AGENT`.
  - `/api/security/fraud/**`, `/api/admin/**` — `ADMIN`.
  - `/api/security/profile/**`, `/api/security/pii/**` (2FA, app-keys, masked PII) — authenticated.
- **Default-secured (auth)**: everything not explicitly public is
  `.anyRequest().authenticated()` — this includes the config, edge and AI surfaces:
  `/api/platform/**` (domains, capabilities, product templates, ingest),
  `/api/edge/**` (rate-limit health + JVM metrics), `/api/ai/search`, `/api/ai/tools`,
  `/api/trips/plan`, `/api/analytics/seat` and `/api/analytics/dynamic-price`.
- **Public (no auth)**: `POST /api/auth/register`, `POST /api/auth/login`,
  `GET /api/tenant/{slug}/branding`, `GET /api/catalog/**`, `GET /api/tickets/verify`,
  `GET /api/referrals/validate`, `POST /api/messaging/webhook/tenant/{tenantId}/channel/{channel}`,
  `GET /api/health/**`, `GET /actuator/health/**`.
- **Rate limiting**, encryption in transit (TLS), tenant isolation and audit logging apply
  in production. Do **not** use the test bootstrap admin credentials in production.

## Error model

All errors return a JSON object with a message and an HTTP status:
`{ "message": "...", "path": "...", "status": 404 }`. Common codes: `400` validation,
`401` unauthorised, `403` forbidden, `404` not found, `409` conflict/business rule,
`429` rate limited, `500` server error.

## Endpoints

### Auth
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/auth/register` | public | Register a user (role default CUSTOMER; tenant optional). |
| POST | `/auth/login` | public | Login -> `{"token":"<JWT>"}`. |

### Catalog (read)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/catalog/search` | public | Unauthenticated public search across providers/shops. |

### Tenant & branding
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/tenant/{slug}/branding` | public | White-label branding to render a shop/tenant theme. |

### Booking / tickets
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/tickets/` | auth | Create booking/reservation (idempotency supported). |
| POST | `/tickets/verify` | public | Verify a QR/barcode ticket. |
| POST | `/tickets/{id}/cancel` | auth | Cancel + refund per policy. |

### Support (24/7)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/support/tickets?tenant={slug}` | auth | Open a ticket. |
| GET | `/support/tickets/me` | auth | My tickets. |
| POST | `/support/tickets/{id}/messages` | auth | Reply. |
| GET | `/support/tickets/{id}/messages` | auth | Conversation. |
| GET | `/support/admin/tickets?tenant={slug}` | ADMIN/AGENT | All tenant tickets. |
| GET | `/support/admin/queue?status=` | ADMIN/AGENT | Queue by status (OPEN, IN_PROGRESS...). |
| GET | `/support/admin/escalated` | ADMIN/AGENT | Escalated tickets. |
| PUT | `/support/admin/tickets/{id}/assign?assignee=` | ADMIN/AGENT | Assign agent. |
| PUT | `/support/admin/tickets/{id}/status?status=` | ADMIN/AGENT | Set CLOSED/RESOLVED/WAITING_CUSTOMER. |
| PUT | `/support/admin/tickets/{id}/escalate` | ADMIN/AGENT | Escalate. |

### Fraud / security (ADMIN)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/security/fraud/check` | ADMIN | Risk-scoring + optional block (0-100). |
| GET | `/security/fraud/signals?tenantId=` | ADMIN | Recent signals. |
| GET | `/security/fraud/high-count` | ADMIN | High-risk signal count. |

### 2FA / security profile (auth)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/security/profile/2fa/otp/send` | auth | Send an email/SMS OTP `{email,purpose}` (REGISTRATION or LOGIN_2FA). |
| POST | `/security/profile/2fa/otp/verify` | auth | Verify an OTP `{email,code}`. |
| POST | `/security/profile/2fa/totp/enable` | auth | Enable RFC 6238 TOTP; returns a Base32 secret `{username}`. |
| POST | `/security/profile/2fa/totp/verify` | auth | Verify a TOTP code `{username,code}` (30s window). |
| POST | `/security/profile/appkey/issue` | auth | Issue an API app-key `{username}`; the key is shown once. |
| POST | `/security/profile/appkey/verify` | auth | Verify an app-key `{username,key}`. |

### Identity verification (auth / ADMIN)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/identity/verify` | auth | Submit identity document `{documentType, documentNumber, documentPhotoUrl}` (NIC / passport / driving licence). |
| GET | `/identity/me` | auth | My verification status. |
| POST | `/admin/identity/{id}/review?approve=` | ADMIN | Approve or reject an identity submission. |

### PII (auth)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/security/pii/me` | auth | Masked view of my PII; raw document numbers / email / phone are never returned. |

### Social (auth; public referral validate)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/family` | auth | Create a family group `{name}`. |
| POST | `/family/{familyId}/join` | auth | Join a family group. |
| DELETE | `/family/{familyId}/members/{userId}` | auth | Remove a member (group owner). |
| GET | `/family` | auth | My family groups. |
| GET | `/family/{familyId}/members` | auth | Members of a group. |
| GET | `/settings` | auth | My preferences (theme/language/currency/notification prefs). |
| PUT | `/settings` | auth | Update preferences. |
| POST | `/referrals` | auth | Generate my referral code. |
| POST | `/referrals/invite` | auth | Invite a friend by email. |
| GET | `/referrals` | auth | My referral codes / status. |
| GET | `/referrals/validate?code=` | public | Validate a referral code. |

### Messaging / omnichannel (auth; webhook public)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/messaging/webhook/tenant/{tenantId}/channel/{channel}` | public | Inbound webhook (WhatsApp / Facebook / Telegram / SMS) for a tenant. |
| POST | `/messaging/send` | auth | Send an outbound message `{channel,recipientRef,body}`. |
| GET | `/messaging` | auth | Conversation log for my tenant. |
| POST | `/messaging/channels/{tenantId}` | ADMIN | Configure a channel integration for a tenant. |

### Payments (auth)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/payments/booking/{bookingId}` | auth | Initiate a payment; method `CARD` / `WALLET` / `PAYPAL` / `BANK`. |
| POST | `/payments/{paymentId}/settle` | auth | Settle / confirm a payment. |
| GET | `/payments/booking/{bookingId}/status` | auth | Check payment status. |

### Analytics / ML / AI (auth)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/ai/search?q=` | auth | AI conversational search: intent detection + `matches[{type,title,id}]`. |
| GET | `/ai/tools` | auth | AI agent tool registry `{role, tools[{name,requiresPermission,description}]}` scoped to the caller's role. |
| GET | `/analytics/forecast/{productId}?horizonDays=` | auth | Demand forecast: daily projections, seasonal factor, confidence. |
| GET | `/analytics/recommend?limit=` | auth | Personalized recommendations (history + popularity). |
| GET | `/analytics/price/{productId}?horizonDays=` | auth | Price projection + surge rate. |
| GET | `/analytics/trend?tenantId=` | auth | Trend report: orders + revenue by domain. |
| GET | `/analytics/anomaly?amount=` | auth | Anomaly score (0-100) + risk level for an amount. |
| POST | `/analytics/ask` | auth | NL data-analyst answer: `{"question":"..."}` -> `{"answer":"..."}`. |
| GET | `/analytics/seat?count=&capacity=&preference=&taken=` | auth | AI seat recommendation `{seats[], comfortScore, reason}`. |
| GET | `/analytics/dynamic-price?basePrice=&demandScore=&capacityRemaining=&capacityTotal=` | auth | Dynamic pricing / surge projection `{basePrice, surgeRate, projectedPrice, guardrailActive}` (1.6x guardrail clamp). |

### Globalization / i18n (auth)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/globalization/translate?tenantId=&locale=&key=` | auth | Look up a translation key. |
| GET | `/globalization/dictionary?tenantId=&locale=` | auth | Full dictionary (key -> value). |
| POST | `/globalization/messages` | auth | Upsert message `{tenantId,locale,key,value}`. |
| POST | `/globalization/rates` | auth | Upsert FX rate `{tenantId,base,target,rate}`. |
| GET | `/globalization/rates?tenantId=` | auth | Currency conversion rates. |
| GET | `/globalization/currencies?tenantId=` | auth | Supported ISO 4217 currencies. |
| GET | `/globalization/languages?tenantId=` | auth | Supported BCP-47 languages. |

### Commerce (auth)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/promotions` | auth | Create promotion (code, discount type/value, min purchase, validity, max uses, domains, kind). Discount types: `PERCENT / FLAT / VOUCHER / OFFER`. |
| GET | `/promotions?tenantId=` | auth | List promotions. |
| POST | `/promotions/{id}/toggle` | auth | Enable/disable promotion `{enabled}`. |
| GET | `/loyalty` | auth | Current user loyalty account (points, tier). |
| POST | `/reviews` | auth | Create review `{productId,rating,title,comment}`. |
| GET | `/reviews/product/{productId}` | auth | Product reviews. |
| GET | `/reviews/product/{productId}/average` | auth | Average rating. |
| GET | `/notifications` | auth | Current user notifications. |
| POST | `/trips` | auth | Create multi-leg trip `{title}`. |
| POST | `/trips/{tripId}/legs` | auth | Add a leg `{bookingId,note}`. |
| GET | `/trips` | auth | My trips. |
| GET | `/trips/{tripId}/legs` | auth | Legs of a trip. |
| GET | `/trips/plan?tenantId=&origin=&destination=&legs=&startDate=` | auth | Automated smart trip plan `{origin, destination, legCount, legs[], totalFare, feasibilityNote}`. |
| GET | `/pricing/{productId}?promoCode=&currency=` | auth | Final price after promo + currency conversion. |
| POST | `/orders` | auth | Place a universal marketplace order `{tenantId,productId,quantity,promoCode?}`. |
| GET | `/orders/mine` | auth | Current user's orders. |
| GET | `/orders?tenantId=` | auth | Orders for the caller's tenant (tenant-scoped). |

### Tenant & moderation (ADMIN)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/admin/tenants` | ADMIN | Create a tenant. |
| GET | `/admin/tenants` | ADMIN | List tenants. |
| GET | `/admin/tenants/{slug}` | ADMIN | Get a tenant. |
| PUT | `/admin/tenants/{slug}` | ADMIN | Update tenant configuration. |
| PUT | `/admin/tenants/{slug}/status?enabled=` | ADMIN | Enable / disable a tenant. |
| PUT | `/admin/tenants/{slug}/moderation?mode=` | ADMIN | Set moderation mode `INSTANT` (auto-activate) or `REVIEW` (admin approval). |
| PUT | `/admin/tenants/{slug}/branding` | ADMIN | Upsert white-label branding. |
| GET | `/tenant/{slug}/branding` | public | Public branding lookup. |

### Admin (ADMIN)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/admin/dashboard` | ADMIN | Platform dashboard. |
| GET | `/admin/shops?status=` | ADMIN | Shop applications by status. |
| PUT | `/admin/shops/{id}?action=` | ADMIN | Approve / suspend a shop. |
| GET | `/admin/providers` | ADMIN | Providers. |
| POST | `/admin/identity/{id}/review` | ADMIN | Review an identity verification. |

### Ops & audit (ADMIN / auth)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/ops/events?limit=` | ADMIN | Domain/outbox event feed. |
| GET | `/ops/disruption?tenantId=` | ADMIN | Disruption report: active disruptions, findings, recovery recommendations. |
| GET | `/audit?tenantId=&limit=` | ADMIN | Audit log entries. |

### Health / ops surface
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/health/live` | public | Liveness. |
| GET | `/actuator/health` | public | Readiness + component health. |

### Platform / config (auth)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/platform/domains` | auth | Config-driven vertical catalog: list of `DomainDefinition` (key, displayName, supportsInventory, supportsTimedSlots, defaultCurrency). |
| GET | `/platform/capabilities?domain=` | auth | Explicit per-domain capability matrix (`Capability`: name, domain, description, category); no `domain` returns the deduplicated overview. |
| GET | `/platform/product-templates?kind=` | auth | No-code product-kind templates (`ProductTemplate`: kind, label, fields[]); no `kind` returns all templates. |
| POST | `/platform/ingest` | auth | Data-platform streaming ingest `{eventType,payload}` -> `{eventType, status:"ACCEPTED"}` (analytics read-side / event-sink surface). |
| GET | `/platform/ingest/pending?limit=` | auth | Pending ingested events, drained in id order (mark processed downstream). |

### Edge / observability (auth)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/edge/health` | auth | Gateway-style edge probe `{status, rateLimits[]}` (configured fixed-window rate-limit rules). |
| GET | `/edge/metrics` | auth | JVM metrics snapshot `{uptimeSeconds, activeThreads, heapUsedBytes, heapMaxBytes}` read from management beans — no extra runtime dependencies. |

## Versioning & stability

- This is the v1.3.0 production contract. Breaking changes require a new major version and
  a documented migration path (see release notes and SDLC).
- Fields marked `nullable` in records may be `null`; never assume the field is present.

## Production notes for integrators

- Use service accounts / OAuth2 client credentials for server-to-server provider calls.
- Webhooks for payment and provider fulfilment are configurable; subscribe and verify
  signatures per your deployment configuration.
- Monitor your SLA: support tickets auto-escalate on SLA breach.
- Integrate the fraud signals endpoint into your ops alerting to catch block spikes early.
