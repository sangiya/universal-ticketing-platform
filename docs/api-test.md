# TicketMesh — API Reference (TEST Environment)

> Test-environment API documentation for TicketMesh. Use this environment to develop and
> validate integrations. Endpoints documented below are the stable public surface for
> v1.3.0. Authentication: most endpoints require a Bearer JWT obtained from `/login`.

## Base URL (test)

```
https://test-api.ticketmesh.example/api
```

## Auth
| Method | Path | Description |
|--------|------|-------------|
| POST | `/auth/register` | Register (optional role + tenant). |
| POST | `/auth/login` | Login -> JWT (`{"token":"..."}`). |

## Catalog (public read)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/catalog/search?q=...` | Universal product search across providers. |

## Tenant & branding (public read for branding)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/tenant/{slug}/branding` | White-label theme for a tenant. |

## Booking / tickets (auth)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/tickets/` | Create booking/reservation. |
| POST | `/tickets/verify` | Verify a QR/barcode ticket. |
| POST | `/tickets/{id}/cancel` | Cancel booking / request refund. |

## Support (auth; admin/agent for admin paths)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/support/tickets?tenant={slug}` | Open a support ticket. |
| GET | `/support/tickets/me` | My tickets. |
| POST | `/support/tickets/{id}/messages` | Reply. |
| GET | `/support/tickets/{id}/messages` | Conversation. |
| GET | `/support/admin/tickets?tenant={slug}` | All tickets for tenant (Admin/Agent). |
| GET | `/support/admin/queue?status=OPEN` | Queue by status. |
| GET | `/support/admin/escalated` | Escalated tickets. |
| PUT | `/support/admin/tickets/{id}/assign?assignee=...` | Assign. |
| PUT | `/support/admin/tickets/{id}/status?status=...` | Update status (CLOSED/RESOLVED/WAITING_CUSTOMER). |
| PUT | `/support/admin/tickets/{id}/escalate` | Escalate. |

## Fraud / security (Admin)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/security/fraud/check` | Evaluate a transaction risk (0-100). |
| GET | `/security/fraud/signals?tenantId=...` | Recent fraud signals. |
| GET | `/security/fraud/high-count` | Count of high-risk signals. |

## 2FA / security profile (auth)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/security/profile/2fa/otp/send` | Send an email/SMS OTP `{email,purpose}`. |
| POST | `/security/profile/2fa/otp/verify` | Verify an OTP `{email,code}`. |
| POST | `/security/profile/2fa/totp/enable` | Enable RFC 6238 TOTP, returns Base32 secret `{username}`. |
| POST | `/security/profile/2fa/totp/verify` | Verify a TOTP code `{username,code}`. |
| POST | `/security/profile/appkey/issue` | Issue an API app-key `{username}` (key shown once). |
| POST | `/security/profile/appkey/verify` | Verify an app-key `{username,key}`. |

## Identity verification (auth; ADMIN review)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/identity/verify` | Submit identity document (NIC / passport / driving licence + photo) `{documentType,documentNumber,documentPhotoUrl}`. |
| GET | `/identity/me` | My verification status. |
| POST | `/admin/identity/{id}/review?approve=true` | Admin approve/reject an identity submission. |

## PII (auth)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/security/pii/me` | Masked view of my PII (raw values never returned). |

## Social (auth, except public referral validate)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/family` | Create a family group `{name}`. |
| POST | `/family/{familyId}/join` | Join a family group. |
| DELETE | `/family/{familyId}/members/{userId}` | Remove a member. |
| GET | `/family` | My family groups. |
| GET | `/family/{familyId}/members` | Members of a group. |
| GET | `/settings` | My preferences (theme/language/currency/notifications). |
| PUT | `/settings` | Update preferences. |
| POST | `/referrals` | Generate my referral code. |
| POST | `/referrals/invite` | Invite a friend `{email}`. |
| GET | `/referrals` | My referral codes / status. |
| GET | `/referrals/validate?code=` | Validate a referral code (public). |

## Messaging / omnichannel (auth; webhook public)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/messaging/webhook/tenant/{tenantId}/channel/{channel}` | Public inbound webhook (WhatsApp / Facebook / Telegram / SMS) `{externalRef,senderRef,body}`. |
| POST | `/messaging/send` | Send outbound message `{channel,recipientRef,body}`. |
| GET | `/messaging` | Conversation log for my tenant. |
| POST | `/messaging/channels/{tenantId}` | Admin configure a channel `{channel,name,apiKeyRef}`. |

## Payments (auth)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/payments/booking/{bookingId}` | Initiate a payment, method `CARD / WALLET / PAYPAL / BANK`. |
| POST | `/payments/{paymentId}/settle` | Settle / confirm a payment. |
| GET | `/payments/booking/{bookingId}/status` | Check payment status. |

## Ops / health (public)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/health/live` | Liveness probe. |
| GET | `/actuator/health` | Readiness/component health. |

## Platform / config (auth)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/platform/domains` | Config-driven vertical catalog (list of `DomainDefinition`: key, displayName, supportsInventory, supportsTimedSlots, defaultCurrency). |
| GET | `/platform/capabilities?domain=` | Explicit per-domain capability matrix (`Capability`: name, domain, description, category); no `domain` returns the deduplicated overview. |
| GET | `/platform/product-templates?kind=` | No-code product-kind templates (`ProductTemplate`: kind, label, fields[]); no `kind` returns all templates. |
| POST | `/platform/ingest` | Data-platform streaming ingest `{eventType,payload}` -> `{eventType,"status":"ACCEPTED"}` (analytics read-side / event-sink surface). |
| GET | `/platform/ingest/pending?limit=` | Pending ingested events drained in id order (mark processed downstream). |

## Edge / observability (auth)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/edge/health` | Gateway-style edge probe `{status, rateLimits[]}` (configured fixed-window rules). |
| GET | `/edge/metrics` | JVM metrics snapshot `{uptimeSeconds, activeThreads, heapUsedBytes, heapMaxBytes}` (management beans, no extra SDK). |

## Analytics / ML / AI (auth)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/ai/search?q=` | AI conversational search — intent detection + matches `{type,title,id}` (`ConversationalSearchResponse`). |
| GET | `/ai/tools` | AI agent tool registry scoped to the caller's role `{role, tools[{name,requiresPermission,description}]}`. |
| GET | `/analytics/forecast/{productId}?horizonDays=7` | Demand forecast (daily projections + confidence). |
| GET | `/analytics/recommend?limit=5` | Personalized product recommendations. |
| GET | `/analytics/price/{productId}?horizonDays=7` | Price projection / surge prediction. |
| GET | `/analytics/trend?tenantId=` | Trend report (orders/revenue by domain). |
| GET | `/analytics/anomaly?amount=1234.56` | Anomaly score + risk level for an amount. |
| POST | `/analytics/ask` | Natural-language question -> data analyst answer (`{"question":"..."}`). |
| GET | `/analytics/seat?count=&capacity=&preference=&taken=` | AI seat recommendation `{seats[], comfortScore, reason}`. |
| GET | `/analytics/dynamic-price?basePrice=&demandScore=&capacityRemaining=&capacityTotal=` | Dynamic pricing / surge projection `{basePrice, surgeRate, projectedPrice, guardrailActive}` (1.6x clamp). |

## Globalization / i18n (auth)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/globalization/translate?tenantId=&locale=&key=` | Translate a key for tenant/locale. |
| GET | `/globalization/dictionary?tenantId=&locale=` | Full translation dictionary. |
| POST | `/globalization/messages` | Upsert message `{tenantId,locale,key,value}`. |
| POST | `/globalization/rates` | Upsert currency rate `{tenantId,base,target,rate}`. |
| GET | `/globalization/rates?tenantId=` | Currency conversion rates. |
| GET | `/globalization/currencies?tenantId=` | Supported currencies. |
| GET | `/globalization/languages?tenantId=` | Supported languages. |

## Commerce (auth)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/promotions` | Create promotion `{tenantId,code,name,discountType,discountValue,minPurchase,startsAt,endsAt,maxUses,domains,kind}`. Discount types: `PERCENT / FLAT / VOUCHER / OFFER`. |
| GET | `/promotions?tenantId=` | List promotions. |
| POST | `/promotions/{id}/toggle` | Enable/disable `{enabled}`. |
| GET | `/loyalty` | Current user loyalty account (points/tier). |
| POST | `/reviews` | Create review `{productId,rating,title,comment}`. |
| GET | `/reviews/product/{productId}` | Product reviews. |
| GET | `/reviews/product/{productId}/average` | Average rating. |
| GET | `/notifications` | Current user notifications. |
| POST | `/trips` | Create trip `{title}`. |
| POST | `/trips/{tripId}/legs` | Add leg `{bookingId,note}`. |
| GET | `/trips` | My trips. |
| GET | `/trips/{tripId}/legs` | Legs of a trip. |
| GET | `/trips/plan?tenantId=&origin=&destination=&legs=&startDate=` | Automated smart trip plan `{origin, destination, legCount, legs[], totalFare, feasibilityNote}`. |
| GET | `/pricing/{productId}?promoCode=&currency=` | Final price with promo + currency conversion. |
| POST | `/orders` | Place universal marketplace order `{tenantId,productId,quantity,promoCode?}`. |
| GET | `/orders/mine` | Current user's orders. |
| GET | `/orders?tenantId=` | Orders for the caller's tenant (tenant-scoped). |

## Tenant & moderation (ADMIN)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/admin/tenants` | Create a tenant. |
| GET | `/admin/tenants` | List tenants. |
| GET | `/admin/tenants/{slug}` | Get a tenant. |
| PUT | `/admin/tenants/{slug}` | Update tenant config (country/currency/language/timezone/domain). |
| PUT | `/admin/tenants/{slug}/status?enabled=` | Enable/disable a tenant. |
| PUT | `/admin/tenants/{slug}/moderation?mode=` | Set moderation mode `INSTANT` (auto-activate shops) or `REVIEW` (admin approval). |
| PUT | `/admin/tenants/{slug}/branding` | Upsert white-label branding `{logoUrl,primaryColor,...}`. |
| GET | `/tenant/{slug}/branding` | Public branding lookup. |

## Admin (ADMIN)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/admin/dashboard` | Platform dashboard stats. |
| GET | `/admin/shops?status=` | List shop applications by status. |
| PUT | `/admin/shops/{id}?action=` | Approve / suspend a shop. |
| GET | `/admin/providers` | List / manage providers. |
| POST | `/admin/identity/{id}/review` | Review an identity verification. |

## Ops (ADMIN) / audit (auth)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/ops/events?limit=10` | Domain/outbox event feed. |
| GET | `/ops/disruption?tenantId=` | Disruption report (active disruptions, findings, recovery recommendations). |
| GET | `/audit?tenantId=&limit=` | Audit log entries. |

## Example: open a support ticket

```
POST /api/support/tickets?tenant=global
Authorization: Bearer <JWT>
Content-Type: application/json

{ "subject": "Payment refund", "category": "REFUND", "priority": "MEDIUM",
  "description": "Charge not reversed" }
```

Response `201` includes a `requestRef` like `SUP-XXXX1234`, an `slaDueAt`, and `status OPEN`.

## Example: fraud check

```
POST /api/security/fraud/check
Authorization: Bearer <ADMIN JWT>
Content-Type: application/json

{ "tenantId":1, "actorUsername":"alice", "subjectType":"BOOKING", "subjectRef":"ref-1",
  "amount":1500.00, "quantity":1, "attemptsInWindow":1, "distinctCardsInWindow":1 }
```

Response `200`: `{"score":0,"risk":"LOW","flags":[],"blocked":false}`.

## Testing notes
- External provider/payment endpoints are stubbed with **WireMock** in the test env, so you
  can run full flows offline with deterministic responses.
- Tests use H2 in MySQL mode with Flyway migrations (`V1..V9`).
- 204 automated tests pass (0 failures, BUILD SUCCESS) across booking, payment, ticket,
  support, fraud, AI, ML/analytics, promotions/vouchers/offers, loyalty, reviews,
  notifications, trips, globalization, pricing, ops, full-flow, the onboarding-security
  batch (2FA/OTP/TOTP/app-keys, identity verification, PII encryption, family, settings,
  referrals, omnichannel messaging and multi-payment gateways), and the new platform
  surfaces (domain registry, capability matrix, product templates, data-platform ingest,
  edge rate limiting + JVM metrics, tracing, conversational search, trip planner, agent
  tools, seat recommendation and dynamic pricing).
