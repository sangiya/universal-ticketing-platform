# TicketMesh — White-label Guide

> How any tenant or agent runs TicketMesh **as their own branded marketplace** — either an
> **own-web** (self-hosted + own domain) or a **hosted managed** build, with per-tenant
> branding and configuration driven entirely by settings, no code changes needed.

## 1. What white-label means here

TicketMesh is a multi-tenant platform. Each tenant is an independent, isolated vertical
with its own:

- **branding** (name, logo, colors, imagery) rendered everywhere by configuration,
- **country / currency / language / timezone**,
- **moderation mode** (`INSTANT` — shops auto-activate and sell immediately; or `REVIEW` —
  new shops await admin approval),
- **messaging channels** (WhatsApp / Facebook / Telegram / SMS),
- **payment gateways** (CARD / WALLET / PAYPAL / BANK),
- **promotions, vouchers & offers**,
- agent and customer data fully isolated from other tenants.

Because branding and behaviour come from tenant configuration rather than code, you can
stand up a new branded marketplace for a partner without a redeploy.

## 2. Two delivery models

### A. Own-web (self-hosted, partner-owned domain)

The partner runs their own instance (see `docs/self-host-deployment.md`) under their own
domain with their own logo/assets. They own the infra, secrets and data.

- **Pros**: full control, data sovereignty, their brand on their traffic.
- **Best for**: large operators who want to own the stack.

### B. Hosted managed service

You run a shared TicketMesh cluster and give each partner a **tenant** scoped to their
brand. Partners get their own subdomain / vanity domain, branded portal, and their own
configuration sliders — mirrored value with zero infra burden.

- **Pros**: no infra for the partner, central ops, quickest to launch a branded store.
- **Best for**: mid-market operators, agents, and rapid partner onboarding.

> A single codebase serves both models — the same `ticketmesh-core` container; the only
> difference is who controls the infrastructure and how tenants are provisioned.

## 3. Per-tenant configuration surface

Admin sets per tenant:

| Area | Where | Notes |
|------|-------|-------|
| Branding | Tenant & Branding admin | name, logo, colors, images |
| Market | Tenant settings | country (ISO 3166-1), currency (ISO 4217), language, timezone |
| Moderation | `PUT /api/admin/tenants/{slug}/moderation?mode=` | `INSTANT` (auto-activate) or `REVIEW` (approve first) |
| Channels | `POST /api/messaging/channels/{tenantId}` | WhatsApp / Facebook / Telegram / SMS, per-tenant API-key refs |
| Payments | gateway availability | CARD / WALLET / PAYPAL / BANK enabled per tenant |
| Promotions | Promotions admin | vouchers / offers / discounts, validity, limits |

## 4. How an agent/partner goes live (INSTANT mode)

1. **Self-register** as an agent (open registration — no invite required).
2. **Provision a shop** — in `INSTANT` mode this is auto-approved and a tenant/provider are
   provisioned automatically, so the shop can start offering products immediately.
3. **Verify identity** (recommended before real transactions): submit NIC / passport /
   driving licence for admin review; PII is encrypted at rest.
4. **Brand the shop** (logo, colors, name) — rendered across catalog, checkout, messaging
   and the storefront.
5. **Connect channels** and optionally a real payment gateway.
6. **Publish products** — customers can search and buy under the tenant's brand.

In `REVIEW` mode the same flow runs, but the shop activation and identity review happen
before the shop can transact.

## 5. Guarding brand integrity

- Set each tenant's **moderation mode** to `REVIEW` if you want to approve shops before
  they appear under your brand.
- Monitor the audit log and ops event feed per tenant (`/api/audit`, `/api/ops/events`) to
  trace who activated shops, approved identities, or changed channel config.
- Enforce 2FA for the tenant's admin accounts (see `docs/security-privacy-guide.md`).

## 6. Data isolation & privacy

- Each tenant's data, sessions and branding are isolated; access is RBAC-scoped
  (`CUSTOMER` / `AGENT` / `ADMIN`).
- All secrets (`JWT_SECRET`, `QR_SECRET`, `PII_MASTER_KEY`) are per-deployment env vars —
  never per-code, never committed.
- For a hosted multi-partner service, consider issuing per-tenant PII keys to further
  isolate encryption domains.

## 7. Selling white-label yourself

Because the platform is a containerized app you can deploy anywhere
(`docs/self-host-deployment.md`), you can:

- **License** it to partners who run their own instances, or
- **Operate** a hosted multi-tenant service and charge per active tenant / per booking.

Both routes reuse the identical core.
