# TicketMesh — API Reference (TEST Environment)

> Test-environment API documentation for TicketMesh. Use this environment to develop and
> validate integrations. Endpoints documented below are the stable public surface for
> v1.0.0. Authentication: most endpoints require a Bearer JWT obtained from `/login`.

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

## Ops / health (public)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/health/live` | Liveness probe. |
| GET | `/actuator/health` | Readiness/component health. |

## Analytics / ML (auth)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/analytics/forecast/{productId}?horizonDays=7` | Demand forecast (daily projections + confidence). |
| GET | `/analytics/recommend?limit=5` | Personalized product recommendations. |
| GET | `/analytics/price/{productId}?horizonDays=7` | Price projection / surge prediction. |
| GET | `/analytics/trend?tenantId=` | Trend report (orders/revenue by domain). |
| GET | `/analytics/anomaly?amount=1234.56` | Anomaly score + risk level for an amount. |
| POST | `/analytics/ask` | Natural-language question -> data analyst answer (`{"question":"..."}`). |

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
| POST | `/promotions` | Create promotion `{tenantId,code,name,discountType,discountValue,minPurchase,startsAt,endsAt,maxUses,domains}`. |
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
| GET | `/pricing/{productId}?promoCode=&currency=` | Final price with promo + currency conversion. |
| POST | `/orders` | Place universal marketplace order `{tenantId,productId,quantity,promoCode?}`. |
| GET | `/orders/mine` | Current user's orders. |
| GET | `/orders?tenantId=` | Orders for the caller's tenant (tenant-scoped). |

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
- Tests use H2 in MySQL mode with Flyway migrations.
- 77 automated tests pass across booking, payment, ticket, support, fraud, AI, ML/analytics,
  promotions, loyalty, reviews, notifications, trips, globalization, pricing, ops and full-flow.
