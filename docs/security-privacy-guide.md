# TicketMesh — Security & Privacy Guide

> Reference for how TicketMesh handles authentication, authorization, two-factor
> authentication, identity verification, PII encryption, secrets and fraud — with honest
> notes on what is fully wired versus offline-simulated so operators know exactly what to
> do before production.

## 1. Authentication & authorization

- **JWT** (HS256, signed with `JWT_SECRET`) issued at login; `JWT_EXPIRATION_MS` controls
  lifetime (default 24h).
- **Roles**: `CUSTOMER`, `AGENT`, `ADMIN`. Authorization is method-level in the API layer.
- **Passwords**: hashed with **BCrypt** at rest — never stored or logged in plaintext.
- Endpoints are public, authenticated, agent/admin, or admin-only as appropriate (see
  `docs/api-production.md` for the exact auth split per route).

## 2. Two-factor authentication (2FA)

`TwoFactorService` supports three factors, all verified against hashed values:

- **Email / SMS OTP** — 6-digit one-time passcode, hashed, 5-minute expiry.
- **TOTP** — RFC 6238, HMAC-SHA1, **30-second** period, **6 digits**, implemented with the
  JDK crypto APIs only (no external TOTP dependency); compatible with standard authenticator
  apps.
- **App keys** — recoverable API app-keys for programmatic access, hashed at rest.

Endpoints: `POST /api/security/profile/setup-2fa`,
`POST /api/security/profile/enable-2fa`, `POST /api/security/profile/verify-2fa` and
related app-key operations.

> **Recommendation:** enforce 2FA for all `ADMIN` and, in production, all `AGENT` accounts.

## 3. Identity verification (KYC)

- An agent submits an `IdentityRequest` with a **document type** (`NIC` / `passport` /
  `driving licence`), **document number** and **document photo URL**.
- Submitted via `POST /api/identity/verify`; reviewable via `GET /api/identity/me` and
  approved/rejected by admins via `POST /api/admin/identity/{id}/review`.
- Verified identity is a prerequisite signal before an agent should transact.

> **Honest note:** in this reference build, documents are stored/uploads referenced by URL
> and validation is config/system-driven rather than calling a real KYC provider (e.g. an
> ID-scan / liveness vendor). Before production, plug in a real document-verification
> provider and, ideally, offload the photo to an object store with signed URLs.

## 4. PII at rest

- Sensitive fields (identity document numbers, phone/copies where marked, contact copies)
  are encrypted with **AES-256/GCM** (`PiiEncryptor`).
- Encryption uses a **random 12-byte IV per record** and a **128-bit** auth tag, keyed by
  `PII_MASTER_KEY` derived from the environment.
- The platform exposes only a **masked** view — `GET /api/security/pii/me` returns masked
  values; raw PII is never returned to clients.

> **Recommendation:** the AES `PII_MASTER_KEY` must be unique, strong, stable and backed up
> — you cannot decrypt PII after losing/rotating it. Use a secrets manager.

## 5. Secrets management

| Secret | Used for | Keep |
|--------|----------|------|
| `JWT_SECRET` | JWTs | random, strong, kept stable across restarts |
| `QR_SECRET` | QR ticket HMAC-SHA256 signatures | random, strong, stable |
| `PII_MASTER_KEY` | AES-256/GCM PII encryption | random, strong, backed up |
| `BOOTSTRAP_ADMIN_PASSWORD` | initial admin | change immediately on first login |
| `DB_PASSWORD` | database | per-environment |

All secrets come from environment variables / a secrets manager — **none are committed**.
Rotate with a documented procedure; PII rotation requires re-encrypting stored records.

## 6. Application / transport security

- **Dates, times and monetary amount** handling is done with typed, validated DTOs;
  global exception handling; input validation on all request bodies.
- **TLS**: always terminate HTTPS at the edge (reverse proxy / LB) in production; the public
  messaging webhook should be TLS-terminated and rate-limited.
- **QR tickets** are HMAC-SHA256 signed (a QR that does not verify is rejected at the gate).

## 7. Fraud & risk

- `/api/security/fraud` is **admin-only**; it computes a 0–100 risk score, raises flags,
  can auto-block, and writes audit signals with admin override via
  `POST /api/admin/fraud/{id}/review`.
- Personality, device/IP and transaction heuristics contribute to the score.

> **Honest note:** fraud scoring here is deterministic/rule-based — it is not a trained
> ML model on real traffic. Before production, calibrate thresholds against your data and
> connect a real payment gateway's risk services.

## 8. Messaging / omnichannel privacy

- Channel integrations (WhatsApp / Facebook / Telegram / SMS) store **API-key references**
  (references, not raw keys) per tenant (`POST /api/messaging/channels/{tenantId}`).
- Inbound public **webhooks** (`POST /api/messaging/webhook/tenant/{tenantId}/channel/{channel}`)
  are **deduplicated by external reference** (idempotent) to prevent replay.
- Messaging transport defaults to an **offline stub**; enable real transport with
  `WHATSAPP_ENABLED=true` + `WHATSAPP_ENDPOINT`.

## 9. Referrals & contacts

- Referral invitee email addresses are stored encrypted (PII) and only used to send the
  invite; invite codes are 8-char and reward 100 loyalty points on join.

## 10. Hardening checklist (production)

- [ ] Change `JWT_SECRET`, `QR_SECRET`, `PII_MASTER_KEY`, bootstrap admin password, DB creds.
- [ ] Migrate secrets to a secrets manager; enforce stable PII key with backups.
- [ ] Enforce 2FA on admin/agent accounts.
- [ ] Require identity verification before agent transactions; connect a real KYC provider.
- [ ] Terminate TLS; rate-limit the public webhook.
- [ ] Review and calibrate fraud thresholds.
- [ ] Restrict DB access to the app user with least privilege.
- [ ] Schedule DB backups and test restore against matching Flyway version.
