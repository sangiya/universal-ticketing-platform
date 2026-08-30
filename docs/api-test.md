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
