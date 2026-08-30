# TicketMesh — API Reference (PRODUCTION)

> Production API documentation for TicketMesh v1.0.0. This is the stable public contract.
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
  - `/api/support/admin/**` — `ADMIN` or `AGENT`.
  - `/api/security/**`, `/api/admin/**` — `ADMIN`.
- **Public (no auth)**: `POST /api/auth/register`, `POST /api/auth/login`,
  `GET /api/tenant/{slug}/branding`, `GET /api/catalog/**`, `GET /api/tickets/verify`,
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

### Ops / health
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/health/live` | public | Liveness. |
| GET | `/actuator/health` | public | Readiness + component health. |

## Versioning & stability

- This is the v1.0.0 production contract. Breaking changes require a new major version and
  a documented migration path (see release notes and SDLC).
- Fields marked `nullable` in records may be `null`; never assume the field is present.

## Production notes for integrators

- Use service accounts / OAuth2 client credentials for server-to-server provider calls.
- Webhooks for payment and provider fulfilment are configurable; subscribe and verify
  signatures per your deployment configuration.
- Monitor your SLA: support tickets auto-escalate on SLA breach.
- Integrate the fraud signals endpoint into your ops alerting to catch block spikes early.
