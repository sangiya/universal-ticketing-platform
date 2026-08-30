# TicketMesh — Admin Portal Manual

> Overall management and monitoring for the platform administrator.

## 1. Sign in

Use your **Admin** credentials. Admin accounts are granted by the platform
(`admin` / configured by `BOOTSTRAP_ADMIN_PASSWORD`, default `ChangeMe123!` — change in
production).

## 2. Tenants (white-label SaaS)

- **Create/configure tenants**: country (ISO 3166-1), currency (ISO 4217), language,
  timezone, domain.
- **Branding**: set logos, colors, images per tenant — rendered everywhere by config.
- Enable/disable tenants; config version bumps refresh downstream caches.

## 3. Marketplace & agents

- **Approve/reject shop applications** submitted by agents.
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

## 8. Monitoring & automation

- **Health**: liveness/readiness and actuator health.
- **Auto-detect**: monitors surface dev/QA/live issues automatically.
- **Auto-fix**: scripted remediation for known issues (see ops runbook).
- Dashboard aggregates system health, support queue, fraud and booking metrics.
