# TicketMesh — Admin Portal Manual

> Overall management and monitoring for the platform administrator.

## 1. Sign in

Use your **Admin** credentials. Admin accounts are granted by the platform
(`admin` / configured by `BOOTSTRAP_ADMIN_PASSWORD`, default `ChangeMe123!` — change in
production).

## 2. Tenants (white-label SaaS)

- **Create/configure tenants**: country (ISO 3166-1), currency (ISO 4217), language,
  timezone, domain.
- **Branding**: set logos, colors, images per tenant — rendered everywhere by config
  (white-label: agents can run their own branded web/app from this).
- **Moderation mode**: per tenant choose `INSTANT` (new shops auto-activate and sell
  immediately — Uber/PickMe model) or `REVIEW` (new shops require admin approval) via
  `PUT /api/admin/tenants/{slug}/moderation?mode=`.
- Enable/disable tenants; config version bumps refresh downstream caches.

## 3. Marketplace & agents

- **Approve/reject shop applications** submitted by agents (required in `REVIEW`
  moderation mode; instant-activated in `INSTANT` mode).
- **Review identity verifications**: approve/reject agent identity documents (NIC /
  passport / driving licence + photo) via
  `POST /api/admin/identity/{id}/review?approve=`.
- **Monitor** all shops, providers and products.
- **Providers**: connect, configure capabilities/catalog integration.

## 4. Users & roles

- Manage customers, agents and admins.
- Assign roles, suspend/activate accounts.

## 5. Catalog

- Browse and search the unified catalog across all providers and shops.

## 6. Support (24/7 portal)

- View all support tickets across tenants.
- **Queue**: work open tickets, **assign** to team members, change status
  (in-progress / waiting-customer / resolved / closed).
- **Escalations**: review auto-escalated (SLA breach) tickets and resolve quickly.

## 7. Fraud & security operations

- Run **fraud checks** on transactions; view **fraud signals**; see the **high-risk count**.
- Review blocked transactions and apply **admin override** where a genuine customer is
  incorrectly blocked.
- Audit trail of security operations.

## 8. Promotions, vouchers & offers

- Create **promotions**, **vouchers** and **offers** per tenant — percentage, flat or
  code-based, with validity windows, minimum purchase and usage caps.
- Enable/disable promotions and monitor redemption; vouchers/offers apply at customer
  checkout via the pricing engine.

## 9. Messaging channels

- **Configure omnichannel channels** (WhatsApp / Facebook / Telegram / SMS) per tenant:
  name + API-key reference. Incoming webhooks are logged and routed to the tenant inbox.
- Monitor message flow and outbound delivery from the messaging surface.

## 10. Staff & audit

- Manage admin/agent staff and roles (`CUSTOMER` / `AGENT` / `ADMIN`); suspend/activate.
- Review the **audit log** (`/api/audit`) and **ops event feed** (`/api/ops/events`) for
  every tenant to trace configuration, moderation and security actions.

## 11. Monitoring & automation

- **Health**: liveness/readiness and actuator health.
- **Auto-detect**: monitors surface dev/QA/live issues automatically.
- **Auto-fix**: scripted remediation for known issues (see ops runbook).
- Dashboard aggregates system health, support queue, fraud and booking metrics.
