# TicketMesh — Spec vs Implementation Gap Analysis

> **Scope reference:** `TicketMesh_Production_Product_Functional_UX_Specification_v1.md`
> **Sections 1–71** define the full platform. This document maps every gap between the
> specification and the actual codebase.

---

## CRITICAL — Application Does Not Start in Offline Mode

The app uses `spring-ai-starter-model-openai` which requires a live OpenAI API call on
startup. The `application.yml` defaults to `spring.profiles.default=ai-offline`, but the
profile simply loads YAML properties — it does NOT replace the `ChatModel` bean. Startup
will fail with a connection error unless `OPENAI_API_KEY` is set.

**Fix needed:** Implement an `@ConditionalOnProperty` offline `ChatModel` bean that returns
hardcoded/offline responses. The existing `AiOfflineConfig.java` exists but is not wired.

---

## CRITICAL — Universal Ticketing Platform Is Train-Only

**Spec sections 9, 13, 17, 18, 230–249** require a **universal multi-domain platform**
(bus, train, movie, events, sports, flight, ferry, attractions). The entire booking flow
is hardcoded to `TrainSchedule` / `TrainRoute`. There is no generic product/ticket domain
model. `BookingService.create()` only handles train schedules. The `TrainController`
(`/api/trains/search`) is the only search endpoint — no `/api/tickets/search`,
`/api/events/search`, etc.

**Affected spec sections:** 9 (Ticket Domain), 13 (Universal Search), 17 (Inventory),
18 (Seat Selection), 230–269.

---

## 1. Authentication — Missing Core Endpoints

| Spec Requirement | Status | Location |
|---|---|---|
| POST /api/auth/logout | ❌ MISSING | `AuthController.java` |
| POST /api/auth/refresh | ❌ MISSING | `AuthService.java` — no refresh token |
| POST /api/auth/forgot-password | ❌ MISSING | `AuthController.java` |
| POST /api/auth/reset-password | ❌ MISSING | `AuthController.java` |
| POST /api/auth/revoke-sessions | ❌ MISSING | spec §1 |
| Session throttling on failed login | ❌ MISSING | `AuthService.java` |
| Role elevation controls | ❌ MISSING | spec §1 |
| Social/federated identity | ❌ MISSING | spec §1 |
| Email/phone normalization (lowercase, E.164) | ❌ MISSING | `RegisterRequest` |

`AuthResponse` only returns `accessToken` — no `refreshToken`, no `expiresIn`,
no `tokenType`. Spec §13 requires OAuth2/OIDC compatible flows with refresh tokens.

---

## 2. Two-Factor Authentication — Controllers Exist, No Flow

| Spec Requirement | Status | Location |
|---|---|---|
| TOTP setup/enable wizard | ⚠️ STUB | `MfaController.java`, `TwoFactorService.java` |
| TOTP verification | ⚠️ STUB | no real TOTP validation logic |
| OTP via Email/SMS | ⚠️ STUB | `MfaController` — endpoints exist, no actual send |
| Recovery codes | ❌ MISSING | spec §2 |
| Step-up auth for sensitive actions | ❌ MISSING | spec §2 |
| Active sessions list | ❌ MISSING | spec §2 |
| Security events audit | ❌ MISSING | spec §2 |

---

## 3. Identity Verification / KYC-KYB — Stubs Only

| Spec Requirement | Status | Location |
|---|---|---|
| Document upload (NIC/passport/license) | ⚠️ STUB | `KycController.java`, `KycService.java` |
| Admin review workflow | ⚠️ STUB | no real approval flow |
| Status: NOT_STARTED → APPROVED lifecycle | ⚠️ STUB | no real state machine |
| KYB (business registration) | ❌ MISSING | spec §3 |
| Provider adapter for automated verification | ❌ MISSING | spec §3 |
| Resumable document upload | ❌ MISSING | spec §3 |

---

## 4. Tenant Creation & Multi-Tenant Isolation

| Spec Requirement | Status | Location |
|---|---|---|
| Tenant creation by Platform Admin | ⚠️ PARTIAL | `TenantController.java` — endpoint exists |
| Tenant slug uniqueness validation | ⚠️ STUB | no global uniqueness check |
| Tenant-scoped query enforcement | ❌ MISSING | no `@PreAuthorize` tenant scoping |
| Cross-tenant access deny-by-default | ❌ MISSING | all queries are tenant-unaware |
| Tenant subdomain + custom domain | ❌ MISSING | spec §4 |
| Tenant disable (stop new sales, preserve read) | ❌ MISSING | spec §4 |
| Tenant wizard: Business → Localization → Branding → Payments | ❌ MISSING | spec §4, §127–128 |

---

## 5. Agent / Shop Self-Onboarding

| Spec Requirement | Status | Location |
|---|---|---|
| Shop application flow | ✅ DONE | `AgentOnboardingService.java` |
| INSTANT vs REVIEW moderation mode | ✅ DONE | `Tenant.ModerationMode` |
| Shop approval/suspension by admin | ✅ DONE | `AgentController.java` |
| Go-Live readiness checklist | ❌ MISSING | spec §5 |
| Multiple shops/branches under one owner | ❌ MISSING | spec §5 |
| Onboarding funnel analytics | ❌ MISSING | spec §5 |
| Staff invitation and role assignment | ❌ MISSING | spec §6 |

---

## 6. Shop, Organization, Branch and Staff Management

| Spec Requirement | Status | Location |
|---|---|---|
| Organization + shop creation | ⚠️ PARTIAL | only AgentShop |
| Staff invitation with expiring tokens | ❌ MISSING | spec §6 |
| Branch-level permissions | ❌ MISSING | spec §6 |
| Shop switcher in header | ❌ MISSING | frontend `App.tsx` |

---

## 7. White-Label Branding & Theme Builder

| Spec Requirement | Status | Location |
|---|---|---|
| Brand name, logo, colors, typography | ✅ DONE | `BrandingService.java`, `TenantBranding` |
| Public branding endpoint | ✅ DONE | `GET /api/tenant/{slug}/branding` |
| Theme preview, versioning, rollback | ❌ MISSING | spec §7 |
| Custom domain verification | ❌ MISSING | spec §7 |
| Feature flags (loyalty, AI, wallet) | ❌ MISSING | spec §7 |
| Three-pane desktop builder UI | ❌ MISSING | spec §7 |

---

## 8. Localization, Currency, Timezone

| Spec Requirement | Status | Location |
|---|---|---|
| Per-tenant language/currency/timezone | ✅ DONE | `Tenant`, `GlobalizationService` |
| Translation dictionaries | ⚠️ STUB | `I18nMessage` model, no real dictionary API |
| Currency conversion abstraction | ⚠️ STUB | `ExchangeRate` model, no real FX service |
| UTC storage + timezone-aware display | ❌ MISSING | all timestamps stored as Instant |
| Right-to-left layout support | ❌ MISSING | spec §8 |

---

## 9. Ticket Domain & Product Templates

| Spec Requirement | Status | Location |
|---|---|---|
| Domain templates (bus/train/movie/events) | ⚠️ STUB | `DomainRegistry.java`, `DomainDefinition.java` |
| Dynamic field schemas per ticket type | ❌ MISSING | spec §9 |
| Draft/save/publish/unpublish lifecycle | ❌ MISSING | spec §9 |
| No-code product template builder | ❌ MISSING | spec §9 |
| **The platform is train-only — no multi-domain templates** | ❌ | spec §9, §231–249 |

---

## 10. Catalog & Service Publishing

| Spec Requirement | Status | Location |
|---|---|---|
| Catalog search with media/description | ✅ DONE | `CatalogService.java`, `CatalogController.java` |
| Publish/unpublish/schedule publish | ⚠️ PARTIAL | enabled flag only |
| Catalog status chips (Draft, In Review, Live) | ❌ MISSING | frontend — no status display |
| Scheduled publish | ❌ MISSING | spec §10 |

---

## 11. Provider Integration & Adapter Framework

| Spec Requirement | Status | Location |
|---|---|---|
| Provider CRUD + capabilities | ✅ DONE | `ProviderService.java` |
| Provider health visibility | ⚠️ STUB | `ProviderOfferClient.java` |
| Circuit breaker per provider | ❌ MISSING | no Resilience4j integration |
| Webhook configuration per provider | ❌ MISSING | spec §11 |
| Parallel multi-provider search | ❌ MISSING | all searches are single-provider |
| Reconciliation job | ❌ MISSING | spec §11 |

---

## 12. Manual, CSV and Bulk Service Upload

| Spec Requirement | Status | Location |
|---|---|---|
| CSV template download | ❌ MISSING | spec §12 |
| Row-level validation | ❌ MISSING | spec §12 |
| Dry-run preview, partial acceptance | ❌ MISSING | spec §12 |
| Idempotent batch import | ❌ MISSING | spec §12 |

---

## 13. Universal Search & Discovery

| Spec Requirement | Status | Location |
|---|---|---|
| Origin/destination, date, passengers search | ⚠️ TRAIN ONLY | `TrainSearchService.java` |
| Multi-provider concurrent search | ❌ MISSING | spec §13 |
| UniversalOffer normalization | ❌ MISSING | no UnifiedOffer model |
| Price/time/duration/seller filters | ❌ MISSING | no filter UI |
| Search context preservation on back navigation | ❌ MISSING | spec §13 |
| Results within p95 <= 2.5s | ❌ NOT TESTED | spec §13 |

---

## 14. AI Conversational Search

| Spec Requirement | Status | Location |
|---|---|---|
| Natural-language → structured fields | ⚠️ STUB | `IntentService.java` — keyword matching only |
| Echo interpreted intent before actions | ❌ MISSING | spec §14 |
| Focus clarification for ambiguous input | ❌ MISSING | spec §14 |
| Parsed chip display (From, To, Date) | ❌ MISSING | frontend — no parsed-chips UI |
| Voice input support | ❌ MISSING | spec §14 |
| **Does not work offline — requires OpenAI API** | ❌ | spec §14 |

---

## 15–16. Recommendation, Personalization, Trip Planner

| Spec Requirement | Status | Location |
|---|---|---|
| ML recommendation engine | ⚠️ STUB | `SeatRecommender.java` — deterministic only |
| Popularity/context-based recommendations | ⚠️ STUB | `MlSuiteService.java` — hardcoded formulas |
| Multi-leg trip creation | ⚠️ STUB | `TripPlannerService.java` — rules-based, no real plan |
| Transfer buffer, impossible overlap detection | ❌ MISSING | spec §16 |
| Timeline itinerary UI | ❌ MISSING | frontend — no trip planner page |

---

## 17–18. Inventory & Seat Selection

| Spec Requirement | Status | Location |
|---|---|---|
| Real-time availability | ⚠️ PARTIAL | `TrainSchedule.availableSeats` — no pub/sub |
| Inventory adjustments with audit | ❌ MISSING | spec §17 |
| Seat map designer (grid/canvas) | ❌ MISSING | spec §18 |
| Customer seat selection UI | ❌ MISSING | `BookingService` auto-allocates seat |
| Seat map version migration | ❌ MISSING | spec §18 |
| Group seating adjacency recommendation | ❌ MISSING | spec §18 |

---

## 19. Reservation Hold & Concurrency

| Spec Requirement | Status | Location |
|---|---|---|
| Temporary hold with expiry | ⚠️ PARTIAL | `Booking.Status.RESERVED`, scheduled expiry |
| Hold expiry timestamp in response | ❌ MISSING | `BookingResponse` has no hold expiry |
| Concurrency protection (optimistic locking) | ⚠️ PARTIAL | `findByIdForUpdate` — not consistently used |
| Idempotent hold endpoint | ❌ MISSING | spec §19 |
| Reservation timer UI during checkout | ❌ MISSING | frontend `PaymentPage` |
| "Check availability again" on expiry | ❌ MISSING | spec §19 |

---

## 20. Universal Booking Engine

| Spec Requirement | Status | Location |
|---|---|---|
| Create booking from validated offer | ⚠️ TRAIN ONLY | `BookingService` — no UniversalOffer |
| Multi-service booking (one booking, many services) | ❌ MISSING | spec §20 |
| Saga/orchestration with compensating actions | ❌ MISSING | no saga pattern |
| Idempotent retry (same idempotency key = same result) | ❌ MISSING | spec §20 |
| Partial multi-leg failure → clear remediation | ❌ MISSING | spec §20 |
| Checkout stepper UI: Review → Travelers → Seats → Payment → Confirm | ❌ MISSING | frontend `PaymentPage.tsx` is just a skeleton |

---

## 21. Agent-Assisted / Counter Sales

| Spec Requirement | Status | Location |
|---|---|---|
| Agent searches and books for customer | ❌ MISSING | spec §21 |
| Guest customer creation | ❌ MISSING | spec §21 |
| Walk-in cash recording | ❌ MISSING | spec §21 |
| Agent resends ticket via email/SMS | ❌ MISSING | spec §21 |
| Fast POS layout for tablet | ❌ MISSING | spec §21 |

---

## 22. Payments & Gateway Abstraction

| Spec Requirement | Status | Location |
|---|---|---|
| CARD / WALLET / PAYPAL / BANK | ✅ DONE | `PaymentGateway` interface + adapters |
| Initiate → authorize → settle flow | ⚠️ PARTIAL | manual settle call needed |
| Tokenized/hosted payment components | ❌ MISSING | spec §22 |
| Async webhook handling | ❌ MISSING | no webhook endpoint for payment callbacks |
| Payment reconciliation job | ❌ MISSING | spec §22 |
| Payment method fee display before selection | ❌ MISSING | frontend `PaymentPage` |

---

## 23. Settlement, Commission & Payout

| Spec Requirement | Status | Location |
|---|---|---|
| Settlement ledger | ✅ DONE | `SettlementEntry` model |
| Platform fee, tenant fee, commission calculation | ❌ MISSING | no `SettlementService` |
| Payout cycles, holds, adjustments | ❌ MISSING | spec §23 |
| Downloadable settlement statements | ❌ MISSING | spec §23 |
| Maker-checker for high-value payouts | ❌ MISSING | spec §23 |
| Finance dashboard (Available/Pending/Paid/Refunds) | ❌ MISSING | frontend — no agent finance page |

---

## 24. Ticket Issuance, QR/Barcode & Wallet

| Spec Requirement | Status | Location |
|---|---|---|
| QR code generation with HMAC-SHA256 signing | ✅ DONE | `TicketService.java` |
| PDF ticket download | ❌ MISSING | spec §24 |
| In-app ticket wallet | ❌ MISSING | frontend `TicketsPage.tsx` is a stub |
| Resend/share ticket | ❌ MISSING | spec §24 |
| Ticket status tracking (ISSUED, USED, CANCELLED) | ⚠️ PARTIAL | `TicketService` only issues, no status tracking |
| Agent ticket printing | ❌ MISSING | spec §24 |

---

## 25. Ticket Validation / Gate Operations

| Spec Requirement | Status | Location |
|---|---|---|
| QR/barcode validation endpoint | ✅ DONE | `GET /api/tickets/verify` |
| Valid / Already Used / Cancelled / Expired responses | ⚠️ PARTIAL | hardcoded responses |
| Manual code entry fallback | ❌ MISSING | spec §25 |
| Validator device/staff/location recording | ❌ MISSING | spec §25 |
| Offline device sync | ❌ MISSING | spec §25 |
| Validator app | ❌ MISSING | no separate validator UI |

---

## 26. Cancellation, Reschedule & Refund

| Spec Requirement | Status | Location |
|---|---|---|
| Booking cancellation | ✅ DONE | `BookingService.cancel()` |
| Full/partial cancellation | ⚠️ PARTIAL | only full cancellation |
| Reschedule/modify booking | ❌ MISSING | spec §26 |
| Cancellation policy evaluation | ❌ MISSING | no policy model |
| Refund amount calculation (fee breakdown) | ❌ MISSING | spec §26 |
| Maker-checker for high-value refunds | ❌ MISSING | spec §26 |

---

## 27. Pricing, Fare, Tax & Fee Engine

| Spec Requirement | Status | Location |
|---|---|---|
| Pricing engine with breakdown | ✅ DONE | `PricingService.java` |
| Price rules by domain/route/schedule/seat | ❌ MISSING | no rule engine |
| Tax/fee rounding per jurisdiction | ❌ MISSING | spec §27 |
| AI dynamic price recommendation | ⚠️ STUB | `DynamicPricingEngine` — not integrated into checkout |
| Price quote with expiry/version | ❌ MISSING | spec §27 |
| IF conditions → THEN action pricing rules UI | ❌ MISSING | spec §27 |

---

## 28. Promotions, Vouchers & Offers

| Spec Requirement | Status | Location |
|---|---|---|
| Create promotion (PERCENT/FLAT/VOUCHER/OFFER) | ✅ DONE | `PromotionService.java` |
| Validate + redeem at checkout | ✅ DONE | `PromotionService.validate/redeem` |
| Campaign performance tracking | ❌ MISSING | spec §28 |
| Promotion stacking (exclusive/best-of) | ❌ MISSING | spec §28 |
| Marketing campaign management UI | ❌ MISSING | frontend — no promotion creation UI |

---

## 29. Loyalty, Wallet Credits & Membership

| Spec Requirement | Status | Location |
|---|---|---|
| Loyalty account with points | ✅ DONE | `LoyaltyService.java`, `LoyaltyLedgerEntry` |
| Tier/membership levels | ⚠️ STUB | `LoyaltyAccount.tier` — no real tier logic |
| Points accrual on booking completed | ❌ MISSING | no booking → loyalty integration |
| Points reversal on refund | ❌ MISSING | spec §29 |
| Wallet with top-up | ✅ DONE | `WalletService.java` |
| Wallet top-up via payment | ⚠️ PARTIAL | `WalletService.topUp()` — stub, no real payment |

---

## 30. Reviews & Ratings

| Spec Requirement | Status | Location |
|---|---|---|
| Review submission | ⚠️ STUB | `ReviewController.java`, `ReviewService.java` |
| Verified booking badge | ❌ MISSING | spec §30 |
| Agent response to reviews | ❌ MISSING | spec §30 |
| Review moderation/reporting | ❌ MISSING | spec §30 |
| Review trends in agent dashboard | ❌ MISSING | spec §30 |

---

## 31–32. Notifications & Omnichannel Messaging

| Spec Requirement | Status | Location |
|---|---|---|
| In-app notification center | ✅ DONE | `NotificationService`, `NotificationController` |
| Email/SMS/push adapters | ⚠️ STUB | `NotificationService` marks SENT immediately |
| WhatsApp/Facebook/Telegram/SMS adapters | ⚠️ STUB | `MessagingService.java` — webhook receiver exists |
| Conversation inbox for agents | ❌ MISSING | spec §32 |
| Quick replies/templates | ❌ MISSING | spec §32 |
| Public webhook for inbound messages | ✅ DONE | `MessagingController.webhook()` |

---

## 33–36. Customer Profile, Family, Settings, Referrals

| Spec Requirement | Status | Location |
|---|---|---|
| Saved travelers/passengers | ✅ DONE | `TravelerService.java` |
| Family groups (create/join/remove) | ✅ DONE | `FamilyGroupService.java` |
| User settings (theme/language/currency) | ⚠️ STUB | `UserSettingsService.java` — model exists |
| Referral code generation + invite | ⚠️ PARTIAL | `ReferralService.java` — generate only |
| Referral reward application on qualifying event | ❌ MISSING | spec §36 |
| PII deletion/export (GDPR) | ❌ MISSING | spec §33 |

---

## 37. My Trips, Orders & Booking History

| Spec Requirement | Status | Location |
|---|---|---|
| Booking history (upcoming/completed/cancelled) | ✅ DONE | `BookingService.listMine()` |
| Marketplace orders | ✅ DONE | `ProductOrderService` |
| Trip grouping (multi-leg) | ⚠️ STUB | `TripService.java` — no real grouping |
| Filter by date/domain/status | ❌ MISSING | frontend `OrdersPage.tsx` — no filters |
| Export/receipt download | ❌ MISSING | spec §37 |

---

## 38. Support & 24/7 Service Desk

| Spec Requirement | Status | Location |
|---|---|---|
| Create support ticket | ✅ DONE | `SupportService.java` |
| SLA due time calculation | ✅ DONE | 1h/4h/24h/72h by priority |
| Auto-escalation on SLA breach | ✅ DONE | `@Scheduled slaCheckMs` |
| Ticket assignment, status, notes | ✅ DONE | `SupportService.assign/updateStatus` |
| Support portal UI | ⚠️ STUB | `SupportPage.tsx` — very basic |
| FAQ/search before case creation | ❌ MISSING | spec §38 |
| Support queue SLA countdown in admin | ❌ MISSING | frontend `AdminPage` |

---

## 39–40. Agent Dashboard & Platform Admin

| Spec Requirement | Status | Location |
|---|---|---|
| Agent dashboard with KPIs | ⚠️ PARTIAL | `AgentPortalPage.tsx` — products/providers only |
| Booking/sales/inventory alerts | ❌ MISSING | spec §39 |
| Trend charts and top services | ❌ MISSING | spec §39 |
| Platform admin dashboard | ⚠️ PARTIAL | `AdminPage.tsx` — overview only |
| GMV, active agents, service health | ⚠️ STUB | `AdminService.java` |
| Fraud alerts, support SLA, incidents | ❌ MISSING | spec §40 |
| System health card with status/latency | ❌ MISSING | spec §40 |

---

## 41. Moderation & Marketplace Governance

| Spec Requirement | Status | Location |
|---|---|---|
| Shop application review (approve/reject) | ✅ DONE | `AgentOnboardingService.approve()` |
| Suspension with reason and audit | ✅ DONE | `ShopModerationAudit` |
| Moderation mode per tenant | ✅ DONE | `Tenant.ModerationMode` |
| Product/service report review | ❌ MISSING | spec §41 |
| Risk badges on moderation queue | ❌ MISSING | spec §41 |

---

## 42. Fraud Detection

| Spec Requirement | Status | Location |
|---|---|---|
| Risk scoring (0–100) | ✅ DONE | `FraudDetectionService.anomalyScore()` |
| Named risk flags | ✅ DONE | `FraudSignal` model |
| ALLOW/CHALLENGE/BLOCK outcomes | ⚠️ STUB | `FraudCheckResponse` — no real action |
| Fraud alert queue in admin | ❌ MISSING | spec §42 |
| Manual override with audit | ❌ MISSING | spec §42 |
| ML model versioning | ❌ MISSING | spec §42 |

---

## 43. Disruption Detection & Recovery

| Spec Requirement | Status | Location |
|---|---|---|
| Disruption report + recovery recommendations | ⚠️ STUB | `DisruptionService.assess()` — rules-based |
| Affected booking identification | ❌ MISSING | spec §43 |
| Automatic customer notification on disruption | ❌ MISSING | spec §43 |
| Incident timeline in ops console | ❌ MISSING | spec §43 |

---

## 44–45. Analytics & AI Data Analyst

| Spec Requirement | Status | Location |
|---|---|---|
| Revenue, booking, conversion dashboards | ⚠️ STUB | `MlSuiteService` — deterministic |
| Provider SLA/latency metrics | ❌ MISSING | spec §44 |
| NL → SQL data analyst | ⚠️ STUB | `AiDataAnalystService` — keyword matching |
| Read-only warehouse query | ❌ MISSING | spec §45 |
| AI analyst UI (chat + chart) | ❌ MISSING | frontend `AnalyticsPage.tsx` is skeleton |
| Suggested prompts by role | ❌ MISSING | spec §45 |

---

## 46–47. Data Platform & Event-Driven Architecture

| Spec Requirement | Status | Location |
|---|---|---|
| Outbox event table | ✅ DONE | `OutboxEvent` model |
| Outbox event publishing | ⚠️ STUB | `EventService.java` — no Kafka |
| Kafka streaming to analytical storage | ❌ MISSING | spec §46 |
| Event schema versioning | ❌ MISSING | spec §46 |
| DLQ (Dead Letter Queue) handling | ❌ MISSING | spec §46 |
| All domain events (ReservationHeld, BookingCreated, etc.) | ❌ MISSING | spec §47 |

---

## 48. API Gateway, Edge & Rate Limiting

| Spec Requirement | Status | Location |
|---|---|---|
| Edge health endpoint | ✅ DONE | `EdgeController.health()` |
| JVM metrics | ⚠️ STUB | `EdgeController.metrics()` — basic only |
| Rate limiting | ⚠️ PARTIAL | `EdgeRateLimiter.java` — in-memory, no persistence |
| API versioning strategy | ❌ MISSING | spec §48 |
| Public/authenticated/partner/admin tiers | ❌ MISSING | spec §48 |
| Developer portal/API docs | ❌ MISSING | spec §48 |

---

## 49–50. API Keys & Authorization

| Spec Requirement | Status | Location |
|---|---|---|
| App key issue/verify | ⚠️ STUB | `SecurityPiiController.java` — app-key related |
| RBAC role templates | ⚠️ PARTIAL | `@PreAuthorize` annotations exist |
| ABAC (tenant/branch/resource scoping) | ❌ MISSING | spec §50 |
| Permissions matrix UI | ❌ MISSING | spec §50 |
| Dangerous permission warnings + MFA | ❌ MISSING | spec §50 |

---

## 51. PII Encryption, Privacy & Data Lifecycle

| Spec Requirement | Status | Location |
|---|---|---|
| AES-256/GCM encryption at rest | ✅ DONE | `PiiEncryptor.java` |
| Masked PII view | ✅ DONE | `PiiService.java` |
| PII redaction before LLM calls | ✅ DONE | `GuardrailService.redact()` |
| Retention/deletion/anonymization | ❌ MISSING | spec §51 |
| GDPR data export request | ❌ MISSING | spec §51 |
| Privacy center UI | ❌ MISSING | spec §51 |

---

## 52–53. AI Agent Runtime & Guardrails

| Spec Requirement | Status | Location |
|---|---|---|
| Tool registry with schemas | ⚠️ STUB | `AgentToolRegistry.java` — no real tools |
| Tool authorization before call | ⚠️ STUB | `AgentAuthorizationService.java` |
| Audit of prompts/tools/results | ❌ MISSING | spec §52 |
| User confirmation before booking/payment | ❌ MISSING | spec §52 |
| Prompt injection defense | ⚠️ PARTIAL | `GuardrailService.blockReason()` |
| LLM evaluation suites | ❌ MISSING | spec §53 |
| Admin AI evaluation dashboard | ❌ MISSING | spec §53 |

---

## 54. Dynamic Pricing

| Spec Requirement | Status | Location |
|---|---|---|
| Surge multiplier calculation | ✅ DONE | `DynamicPricingEngine.java` |
| Demand forecasting | ⚠️ STUB | `DemandForecast.java` — hardcoded |
| Price prediction | ⚠️ STUB | `PricePrediction.java` — hardcoded |
| Dynamic price applied at checkout | ❌ MISSING | `PricingService` doesn't call `DynamicPricingEngine` |
| Agent revenue page with forecast chart | ❌ MISSING | spec §54 |
| Customer "Price may rise" indicator | ❌ MISSING | spec §54 |

---

## 55. Observability, Logging, Tracing & Metrics

| Spec Requirement | Status | Location |
|---|---|---|
| Spring Boot Actuator endpoints | ✅ DONE | `application.yml` exposes health/info/metrics/prometheus |
| Deterministic trace IDs | ✅ DONE | `TraceService.java` |
| Prometheus metrics endpoint | ⚠️ STUB | `EdgeController.metrics()` — not real Micrometer |
| Structured logging with traceId/spanId/correlationId | ❌ MISSING | no MDC in logging |
| Distributed tracing (Jaeger/Zipkin) | ❌ MISSING | spec §55 |
| SLOs and alerts | ❌ MISSING | spec §55 |

---

## 56–57. Operational Health, Auto-Recovery & Backup

| Spec Requirement | Status | Location |
|---|---|---|
| Liveness/readiness health | ✅ DONE | Spring Actuator |
| Reservation expiry auto-release | ✅ DONE | `ReservationReleaseScheduler.java` |
| SLA breach auto-escalation | ✅ DONE | `SupportService.autoEscalateSlaBreaches()` |
| Automated safe remediation | ❌ MISSING | spec §56 |
| Incident log and runbooks | ❌ MISSING | spec §56 |
| Backup/restore testing documentation | ❌ MISSING | spec §57 |

---

## 58–60. Deployment & Infrastructure

| Spec Requirement | Status | Location |
|---|---|---|
| Docker + docker-compose | ✅ DONE | `Dockerfile`, `docker-compose.yml` |
| Kubernetes + HPA | ✅ DONE | `infra/kubernetes/*.yaml` |
| Helm chart | ✅ DONE | `infra/helm/ticketmesh/` |
| Terraform AWS ECS | ✅ DONE | `infra/terraform/main.tf` |
| Argo CD / GitOps | ❌ MISSING | spec §59 |
| KEDA for Kafka consumers | ❌ MISSING | spec §59 |
| Network Policies | ❌ MISSING | spec §59 |
| Progressive delivery (Argo Rollouts) | ❌ MISSING | spec §59 |

---

## 61. CI/CD & DevSecOps Pipeline

| Spec Requirement | Status | Location |
|---|---|---|
| Basic CI workflow | ✅ DONE | `.github/workflows/ci.yml` |
| SBOM generation (Syft) | ❌ MISSING | spec §61 |
| Container scanning (Trivy/Grype) | ❌ MISSING | spec §61 |
| Secret scanning (Gitleaks) | ❌ MISSING | spec §61 |
| IaC scanning (Checkov) | ❌ MISSING | spec §61 |
| DAST (OWASP ZAP) | ❌ MISSING | spec §61 |
| Progressive production rollout | ❌ MISSING | spec §61 |
| Smoke tests + rollback | ❌ MISSING | spec §61 |

---

## 62. Testing Strategy

| Spec Requirement | Status | Location |
|---|---|---|
| Unit tests (JUnit 5 + Mockito) | ✅ DONE | 40+ test classes |
| Integration tests | ✅ DONE | `FullFlowIntegrationTest.java` |
| WireMock provider contract tests | ⚠️ PARTIAL | `ProviderOfferClientWireMockTest.java` |
| Architecture tests (ArchUnit) | ❌ MISSING | spec §62 |
| REST Assured API tests | ❌ MISSING | spec §62 |
| Testcontainers for MySQL | ❌ MISSING | spec §62 |
| Pact contract tests | ❌ MISSING | spec §62 |
| Playwright E2E tests | ❌ MISSING | spec §62 |
| k6 performance tests | ❌ MISSING | spec §62 |

---

## 63. WireMock Provider Simulators

| Spec Requirement | Status | Location |
|---|---|---|
| Mock bus/train/movie/event providers | ⚠️ TRAIN ONLY | `ProviderOfferClientWireMockTest.java` |
| Timeout, rate-limit, error scenarios | ❌ MISSING | spec §63 |
| Pact contract validation | ❌ MISSING | spec §63 |

---

## 64. Performance, Scalability & Reliability

| Spec Requirement | Status | Location |
|---|---|---|
| Load profile definition | ❌ MISSING | spec §64 |
| SLO definition and testing | ❌ MISSING | spec §64 |
| Idempotent critical write APIs | ❌ MISSING | spec §64 |
| Backpressure and queue limits | ❌ MISSING | spec §64 |
| Graceful degradation | ❌ MISSING | spec §64 |

---

## 65–66. Accessibility & Design System

| Spec Requirement | Status | Location |
|---|---|---|
| WCAG 2.2 AA accessibility | ❌ NOT TESTED | spec §65 |
| Keyboard navigation | ❌ MISSING | spec §65 |
| Responsive design | ⚠️ PARTIAL | basic CSS only |
| Dark mode support | ❌ MISSING | spec §66 |
| Design token system | ❌ MISSING | spec §66 |
| White-label theme tokens | ⚠️ PARTIAL | branding only |

---

## 67–70. Customer/Agent/Admin UI

| Spec Requirement | Status | Location |
|---|---|---|
| Customer Web/PWA | ⚠️ PARTIAL | `HomePage`, `MarketplacePage`, `TicketsPage` — basic |
| Customer Mobile app | ❌ MISSING | spec §68 |
| Agent Web (full operations) | ⚠️ PARTIAL | `AgentPortalPage.tsx` — CRUD only |
| Agent Mobile app | ❌ MISSING | spec §68 |
| Platform Admin UX | ⚠️ PARTIAL | `AdminPage.tsx` — overview tables only |
| Responsive PWA installability | ❌ MISSING | no real service worker |

---

## Summary: Priority Fixes

### P0 — Application broken / won't start
1. Wire `AiOfflineConfig` properly with `@ConditionalOnProperty` so ChatModel works offline
2. Remove or replace `spring-ai-starter-model-openai` dependency

### P1 — Core booking flow is train-only
3. Replace `TrainSchedule`-only booking with generic `ProductOrder` as primary
4. Fix `BookingService` to use `ProviderProduct` not `TrainSchedule`
5. Remove `TrainController` — replace with universal `CatalogController`
6. Wire AI conversational search properly with offline fallback

### P2 — Authentication gaps
7. Add logout endpoint
8. Add password reset flow
9. Add refresh token support
10. Add session throttling on failed logins

### P3 — Frontend incomplete
11. Build real `TicketsPage.tsx` (wallet with QR display)
12. Build real `SupportPage.tsx`
13. Build real `PaymentPage.tsx` (with reservation timer)
14. Wire all missing API calls in `HomePage.tsx`
15. Add `AnalyticsPage.tsx` with ML dashboard
16. Fix mobile bottom nav labels
17. Build `SettingsPage.tsx` with real preferences
18. Build `OrdersPage.tsx` with filters

### P4 — Integration gaps
19. Wire `DynamicPricingEngine` into checkout pricing
20. Add Micrometer/Prometheus metrics export
21. Add structured logging with MDC (traceId, tenantId)
22. Add Kafka producer for outbox events
23. Add WireMock stubs for all provider types (bus, movie, events, etc.)
24. Build provider circuit breaker with Resilience4j
25. Add JWT refresh token rotation

### P5 — Testing gaps
26. Add Testcontainers-based integration tests
27. Add ArchUnit architecture tests
28. Add REST Assured API contract tests
