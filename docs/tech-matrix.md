# TicketMesh — Technology Matrix

> Part 54 of the master blueprint. Maps every concern in the platform to the chosen
> technology, the reason, and where it is implemented. This is the single reference for
> the runtime stack, so engineers can answer "what tech does X use?" in one place.

## Legend

- **BUILT** — shipped and used in this repository (compiles, tests green).
- **ADAPTER** — an abstraction / interface surface is present; the concrete backend is
  pluggable (e.g. payment processors, AI providers).
- **READY** — config/infra/tooling exists to adopt in deployment without code change.

## Application layer

| Concern | Technology | Status | Where |
|---------|-----------|--------|-------|
| Language / runtime | Java 21 (LTS), JVM | BUILT | `pom.xml`, `src/` |
| Web framework | Spring Boot 3.3.5 (embedded Tomcat) | BUILT | `pom.xml` |
| Dependency injection | Spring Core `@Component`/`@Service` | BUILT | `src/main/java/com/ticketmesh` |
| REST API | Spring MVC (`@RestController`), validated DTOs | BUILT | `controller/`, `dto/` |
| Security | Spring Security + method-level `@PreAuthorize` | BUILT | `config/SecurityConfig.java` |
| Authentication | JWT bearer (JJWT 0.12.6), stateless | BUILT | `security/` |
| Password hashing | BCrypt (`BCryptPasswordEncoder`) | BUILT | `config/`, `service/AuthService.java` |
| Authorization model | RBAC roles `CUSTOMER` / `AGENT` / `ADMIN` + tenant isolation | BUILT | `model/User.java`, `service/*` |
| Validation | Jakarta Bean Validation (`jakarta.validation`) | BUILT | `dto/*Request.java` |
| Error handling | `@RestControllerAdvice` global handlers, typed exceptions | BUILT | `web/` (exception handler) |

## Data layer

| Concern | Technology | Status | Where |
|---------|-----------|--------|-------|
| ORM | Spring Data JPA / Hibernate | BUILT | `model/`, `repository/` |
| Primary database | MySQL 8 (runtime), PostgreSQL-compatible SQL | BUILT | `application.yml`, `docker-compose.yml` |
| Test database | H2 (MySQL compatibility mode) | BUILT | `application.yml` (test profile) |
| Schema migration | Flyway (versioned migrations) | BUILT | `src/main/resources/db/migration/V1..V6` |
| Transaction mgmt | `@Transactional`, pessimistic locks for inventory | BUILT | `service/ReservationService`, `ProductOrderService` |
| Multi-tenancy | per-tenant row scoping + tenant entity (ISO 3166-1, ISO 4217, BCP-47) | BUILT | `model/Tenant.java` |
| Auditing | audit log table + event/outbox table | BUILT | `V5/V6` migrations, `AuditService`, `EventService` |

## Domain / business services

| Concern | Technology | Status | Where |
|---------|-----------|--------|-------|
| Catalog & universal search | in-memory normalize + repository query | BUILT | `CatalogService`, `CatalogController` |
| Provider adapter | canonical HTTP client + WireMock stubs | BUILT / ADAPTER | `integration/ProviderOfferClient.java` |
| Booking / reservation | DB-backed hold/expiry + pessimistic lock + idempotency | BUILT | `service/ReservationService` |
| Payments | multi-payment gateway abstraction — CARD / WALLET / PAYPAL / BANK (offline stub backends) | BUILT / ADAPTER | `service/PaymentService`, `integration/PaymentGatewayRegistry` |
| Refund & cancellation | policy-driven, seat release, event emission | BUILT | `BookingService` |
| Ticket issuing | ZXing QR + HMAC-SHA256 signed payload + verify | BUILT | `service/TicketService` |
| Pricing / fares | transparent base + tax + service-fee breakdown + FX conversion | BUILT | `PricingService` |
| Promotions | promo engine (PERCENT / FLAT / **VOUCHER** / **OFFER** kinds, validity, limits) | BUILT | `PromotionService`, `PricingService` |
| Loyalty | points / earn / redeem / tiers | BUILT | `LoyaltyService` |
| Family groups | create / join / remove-member / list | BUILT | `FamilyGroupService`, `FamilyController` |
| Settings | per-user theme / language / currency / notification prefs | BUILT | `UserSettingsService`, `SettingsController` |
| Referrals | invite codes, invite-by-email, loyalty rewards on join | BUILT | `ReferralService`, `ReferralController` |
| Vouchers / offers | promotion `kind` VOUCHER / OFFER + PERCENT / FLAT | BUILT | `Promotion`, `PromotionService` |
| Messaging (omnichannel) | WhatsApp / Facebook / Telegram / SMS webhooks + outbound | BUILT / ADAPTER | `MessagingService`, `integration/MessagingDispatcher` |
| Support | 24/7 portal, SLA, assignment, escalation | BUILT | `SupportService` |
| Compliance (SLA/audit) | scheduled SLA-breach escalation + audit trail | BUILT | `service/*`, scheduler |

## Security & privacy

| Concern | Technology | Status | Where |
|---------|-----------|--------|-------|
| Two-factor (OTP) | email/SMS one-time passcode, hashed, 5-min expiry | BUILT | `TwoFactorService`, `OtpCode` |
| Two-factor (TOTP) | RFC 6238, HMAC-SHA1, 30s period, 6 digits, **JDK-only** | BUILT | `TwoFactorService`, `Base32` |
| App keys | recoverable API app-key (hashed at rest) | BUILT | `TwoFactorService` |
| Identity verification | NIC / passport / driving licence + photo, admin review | BUILT | `IdentityVerificationService`, `IdentityController` |
| PII encryption | **AES-256/GCM**, random 12-byte IV per record | BUILT | `PiiEncryptor`, `PiiService` |
| Masked PII | masked view only — raw values never returned | BUILT | `SecurityPiiController` (`/api/security/pii/me`) |
| Multi-payment gateways | CARD / WALLET / PAYPAL / BANK (offline stub) | BUILT / ADAPTER | `integration/PaymentGatewayRegistry` + gateways |
| Messaging channels | WhatsApp / Facebook / Telegram / SMS webhook + outbound | BUILT / ADAPTER | `MessagingDispatcher`, `WhatsAppMessagingAdapter` |
| Moderation / open registration | per-tenant `INSTANT` / `REVIEW` activation | BUILT | `Tenant.ModerationMode`, `AgentOnboardingService` |
| Secrets management | env vars / secret manager only (`JWT_SECRET`, `QR_SECRET`, `PII_MASTER_KEY`); never committed | BUILT | `application.yml`, infra secrets |

## AI / ML / analytics

| Concern | Technology | Status | Where |
|---------|-----------|--------|-------|
| Deterministic ML-style suite | pure-Java heuristics (forecast, price, recommend, anomaly, trend, analyst) | BUILT | `com.ticketmesh.ml` |
| AI assistant + RAG | async LLM client adapter + retrieval guardrails | BUILT / ADAPTER | `service/AiAssistant*`, `ai/` |
| NL data analyst | question -> deterministic SQL/aggregation answer | BUILT | `AiDataAnalystService` |
| Fraud detection | TPA/multi-strategy scoring 0-100 + flags + auto-block | BUILT | `FraudDetectionService` |

## Globalization

| Concern | Technology | Status | Where |
|---------|-----------|--------|-------|
| Multi-currency | FX rate table, ISO 4217 code validation | BUILT | `GlobalizationService`, `PricingService` |
| Multi-language | BCP-47 translation dictionaries | BUILT | `GlobalizationService` |
| Timezone | per-tenant timezone (IANA) | BUILT | `Tenant` |

## Frontend

| Concern | Technology | Status | Where |
|---------|-----------|--------|-------|
| Framework | React 18 + TypeScript (strict) | BUILT | `frontend/` |
| Build tooling | Vite | BUILT | `frontend/vite.config.*` |
| PWA | Workbox/VitePWA (installable, offline) | BUILT | `frontend/` |
| Routing | React Router | BUILT | `frontend/src/App.tsx` |
| HTTP client | fetch-based typed client | BUILT | `frontend/src/api/client.ts` |
| Channels | consumer Web + PWA + admin portal + agent/shop portal | BUILT | `frontend/src/pages/*` |
| White-label theming | per-tenant branding rendered from config | BUILT | `TenantBranding`, consumer pages |

## Testing

| Concern | Technology | Status | Where |
|---------|-----------|--------|-------|
| Unit tests | JUnit 5 + Mockito (mock-maker-inline) | BUILT | `src/test/` |
| Provider contract | WireMock stubs | BUILT | `src/test/resources/` + integration tests |
| Full-flow / integration | context tests + H2 | BUILT | `src/test/` |
| Frontend type-check/build | `tsc -b && vite build` gate | BUILT | `frontend/` (CI) |
| Coverage | JaCoCo (associating gate) | READY | `pom.xml` |

## Infrastructure / deployment

| Concern | Technology | Status | Where |
|---------|-----------|--------|-------|
| Container | multi-stage non-root Dockerfile | BUILT | `Dockerfile` |
| Local compose | docker-compose (MySQL + app) | BUILT | `docker-compose.yml` |
| Kubernetes | Deployment + Service + HPA + ConfigMap (autoscaling) | BUILT | `infra/kubernetes/*` |
| Helm | chart with HPA, secrets, service account | BUILT | `infra/helm/ticketmesh/*` |
| AWS | Terraform: ECS Fargate + target-tracking auto scaling + CloudWatch | BUILT | `infra/terraform/*` |
| CI/CD | GitHub Actions: `ci.yml`, `security.yml`, `release.yml` | BUILT | `.github/workflows/` |
| Security scanning | Gitleaks, Checkov, OWASP dependency check | BUILT | `security.yml` |
| Observability | Actuator health/readiness + Micrometer + Prometheus | BUILT | `application.yml`, `HealthController` |

## Interoperability / standards

| Concern | Standard | Where |
|---------|----------|-------|
| Country codes | ISO 3166-1 alpha-2 | `Tenant`, `Shop` |
| Currency | ISO 4217 | `Tenant`, `Provider`, `Product` |
| Language | BCP-47 | `Tenant`, `GlobalizationService` |
| Timezone | IANA tz database | `Tenant` |
| Auth token | JWT (RFC 7519) | `security/` |
| QR ticket | ZXing, HMAC-SHA256 payload | `TicketService` |
| Contract testing | WireMock stubs (provider-as-contract) | tests |
