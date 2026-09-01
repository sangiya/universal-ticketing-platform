# TicketMesh - Production Product Requirements, Functional Specification & UI/UX Behaviour

This file is the developer-friendly companion to the Word specification. The uploaded Master Requirement Register remains the source baseline; production-detail recommendations below elaborate it.

## Detailed Feature Catalogue
### 1. Identity, Registration and Sign-in
**Source baseline:** Requirements include authentication, customer/agent/admin roles, Spring Security and secure account access.
**Actors:** Customer, Agent/Shop Owner, Agent Staff, Tenant Admin, Platform Admin

**Functional requirements**
- Allow registration using email and/or mobile number according to tenant policy.
- Support password login and extensible social/federated identity without coupling core identity to one provider.
- Issue access/refresh tokens through OAuth2/OIDC compatible flows.
- Expose role-appropriate post-login landing experiences.
- Support account lock, unlock, password reset, session revocation and logout-all-devices.

**Business logic / conditions**
- Normalize email to lowercase and phone numbers to E.164 before uniqueness checks.
- Apply tenant-aware registration rules; a shared-network user may belong to multiple shops/tenants while retaining one identity.
- Privileged roles cannot be self-assigned; role elevation requires an authorized administrator workflow.
- Repeated failed sign-ins trigger progressive throttling and temporary lockout; avoid disclosing whether an account exists.

**Validation / errors**
- Reject weak/common passwords and invalid/duplicate identifiers.
- Handle expired/invalid reset tokens with a safe re-request flow.
- Show generic credential errors and field-specific format errors.

**Security / privacy**
- Passwords are salted/hashed using a modern adaptive password hash; never logged.
- Rotate refresh tokens and revoke token families on suspicious reuse.
- Privileged sessions require stronger authentication and shorter idle/session lifetimes.

**Analytics / telemetry**
- Track registration conversion, login success/failure, reset completion and lockouts without logging secrets.

**Non-functional**
- Authentication p95 response target <= 500 ms excluding external identity provider latency.
- Identity service target availability >= 99.95% in managed production.

**Acceptance criteria**
- A new customer can register, verify and sign in without administrator intervention.
- An agent cannot access shop management until required onboarding gates pass.

**UI / UX**
- Use a clean two-column desktop sign-in layout and single-column mobile flow.
- Primary CTA is visually dominant; alternate login methods remain secondary.
- Show password requirements inline before submission, not only after failure.
- Agent registration includes a visible progress indicator: Account > Business > Verification > Shop > Ready.
- After sign-in, route Customer to Home/My Trips, Agent to Shop Dashboard and Platform Admin to Operations Overview.

### 2. Two-Factor Authentication and Security Profile
**Source baseline:** Uploaded security guide and API references include OTP/TOTP/2FA security profile capabilities.
**Actors:** All authenticated users; mandatory for privileged roles

**Functional requirements**
- Provide TOTP authenticator setup, backup/recovery codes and policy-controlled OTP channels.
- Allow users to view active 2FA methods, trusted sessions and security events.
- Require step-up authentication for sensitive actions such as payout changes, API key creation and high-risk refunds.

**Business logic / conditions**
- Platform Admin, Tenant Admin and security-sensitive Agent roles must have MFA enforced in production.
- Recovery code is single-use and must be regenerated after use or reset.
- Changing primary email/phone, payout destination or MFA method requires re-authentication.

**Security / privacy**
- Rate-limit OTP verification and setup attempts.
- Store TOTP secret encrypted at rest and never expose after enrollment.
- Log enrollment, disablement, failed challenges and recovery usage to immutable audit trail.

**Acceptance criteria**
- Privileged account is blocked from privileged functions until MFA requirement is satisfied.

**UI / UX**
- Security page uses cards for Password, MFA, Active Sessions, Login History and Recovery Codes.
- MFA setup is a guided wizard with QR code, secret fallback, confirmation code and recovery-code download step.
- Use clear risk language and never expose full recovery codes after the initial display.

### 3. Identity Verification / KYC-KYB
**Source baseline:** Requirements include identity document submission, admin review, open agent registration and strong onboarding security.
**Actors:** Agent/Shop Owner, Tenant/Platform Compliance Admin

**Functional requirements**
- Collect configurable person/business verification data based on operating country.
- Support document type, number, document image/reference, selfie/photo where policy requires and business registration fields for KYB.
- Support manual review plus provider-adapter integration for automated verification later.
- Expose status: NOT_STARTED, SUBMITTED, IN_REVIEW, APPROVED, REJECTED, EXPIRED, REQUIRES_MORE_INFO.

**Business logic / conditions**
- Verification requirements are policy-driven by tenant, country, seller type and risk tier.
- Rejection must have internal reason and safe customer-facing reason; resubmission can be permitted.
- A shop may be created in draft before approval but cannot sell if the active moderation/KYC policy requires approval.

**Security / privacy**
- Identity documents require stricter access controls, encryption, retention schedules and audit.
- Never expose raw document numbers in standard logs or broad admin lists.

**Non-functional**
- Large uploads use direct/object storage patterns and resumable upload where needed.

**UI / UX**
- Use a checklist-style verification center with progress, document status and next action.
- Image/document upload shows file constraints, preview and delete/replace controls.
- Admin review screen presents masked identity data, submitted documents, risk indicators and explicit Approve/Reject/Request Info actions.

### 4. Tenant Creation and Multi-Tenant Isolation
**Source baseline:** Requirements explicitly require multi-tenancy SaaS, tenant configuration and tenant-scoped data.
**Actors:** Platform Admin, Tenant Owner/Admin

**Functional requirements**
- Create, enable, disable and configure tenants with slug, legal name, country, base currency, languages, timezone and deployment profile.
- Every tenant owns branding, shops, products, policies, users/roles, integrations and analytics scope.
- Support shared database logical isolation and dedicated deployment/data profiles.

**Business logic / conditions**
- Every tenant-scoped query and event must carry tenant context from authenticated claims, trusted routing metadata or internal service context.
- Tenant slug must be globally unique within a managed deployment.
- Disabling a tenant stops new sales while preserving read-only operational access for authorized admins.

**Security / privacy**
- Cross-tenant access is deny-by-default and explicitly regression-tested.
- Caches, Kafka messages, analytics partitions and object-store paths must carry tenant boundaries.

**Acceptance criteria**
- A user from Tenant A cannot enumerate or access Tenant B resources by changing an identifier.

**UI / UX**
- Platform Admin tenant list includes status, country, plan, shops, users, bookings, health and security indicators.
- Tenant setup wizard follows Business > Localization > Branding > Payments > Policies > Go Live.

### 5. Agent / Ticket Shop Self-Onboarding
**Source baseline:** Master requirements define Uber/PickMe-style open registration where sellers can join without owning a website.
**Actors:** Agent/Shop Owner, Platform/Tenant Moderation Admin

**Functional requirements**
- Allow an eligible seller to register, create organization/shop profile, complete verification, configure payout and start adding ticket services from web or Agent App.
- Support moderation modes INSTANT and REVIEW.
- Show onboarding readiness checklist and blocking reasons.
- Allow multiple shops/branches under one owner where policy permits.

**Business logic / conditions**
- INSTANT mode activates a shop automatically only after mandatory identity/security/payout checks succeed.
- REVIEW mode keeps shop PENDING_REVIEW until approved.
- Suspended shop cannot create new sellable inventory or accept orders; existing customer tickets remain visible and supportable.

**Analytics / telemetry**
- Track funnel abandonment by onboarding step and time-to-first-published-service.

**Acceptance criteria**
- A verified seller can self-onboard and publish a valid service without developer involvement.

**UI / UX**
- Agent onboarding is mobile-first, wizard-based and can be resumed.
- Dashboard hero shows Go-Live readiness percentage and missing actions.
- Use plain language such as “Add your first service” rather than technical provider terminology for small sellers.

### 6. Shop, Organization, Branch and Staff Management
**Source baseline:** Agent manual includes shop building and staff; roles require scoped management.
**Actors:** Shop Owner, Shop Manager, Counter Staff

**Functional requirements**
- Create organization, shop and optional branches/counters.
- Invite staff and assign role templates/permissions scoped to organization, shop or branch.
- Configure shop contact details, operating hours, support contacts and service availability.

**Business logic / conditions**
- Owner cannot remove the last active owner without ownership transfer.
- Staff access is constrained to assigned branches and permissions.
- A branch can be disabled without deleting historical bookings.

**Security / privacy**
- Staff invitation tokens expire and are single-use.
- Sensitive staff role changes require step-up authentication and audit.

**UI / UX**
- Agent Web left navigation: Dashboard, Services, Inventory, Bookings, Customers, Staff, Payments/Settlements, Reports, Marketing, Settings.
- Shop switcher appears in header for owners managing multiple shops.
- Staff page uses searchable table on desktop and cards on mobile.

### 7. White-Label Branding and Theme Builder
**Source baseline:** White-label guide defines custom logo, colors, domain, locale and own-web/managed models; user requested WordPress-like configuration.
**Actors:** Tenant Owner/Admin, Shop Owner where delegated

**Functional requirements**
- Configure brand name, logos, favicon/app icon, colors, typography presets, images, navigation, homepage sections, support details and legal links.
- Support theme draft, preview, versioning, publish and rollback.
- Support tenant subdomain and custom domain verification.
- Support feature flags for loyalty, AI assistant, reviews, wallet, promotions and other optional modules.

**Business logic / conditions**
- Theme configuration is data, not a source-code fork.
- Only validated design tokens and safe content blocks are accepted.
- Publishing a theme creates an immutable version; rollback changes active pointer instead of destructive overwrite.

**Security / privacy**
- Sanitize HTML/rich text and disallow arbitrary executable scripts in tenant content.
- Custom domain ownership must be verified before activation.

**Non-functional**
- Theme changes should appear within 60 seconds globally after publish, with cache invalidation.

**UI / UX**
- Builder uses three-pane desktop layout: left component tree/settings, center live responsive preview, right design/property panel.
- Provide device preview toggles for Desktop, Tablet and Mobile.
- Color inputs show contrast warnings and accessible suggested alternatives.
- Use brand preview on customer header, buttons, cards, ticket wallet and agent login.

### 8. Localization, Language, Currency and Timezone
**Source baseline:** Requirements mandate global any-country, currency and language use; globalization APIs provide dictionaries and rates.
**Actors:** Customer, Agent, Tenant Admin

**Functional requirements**
- Configure supported languages, default locale, currencies, timezone and formatting by tenant.
- Allow per-user language/currency preference where supported.
- Maintain translation dictionaries with tenant overrides.
- Use currency conversion abstraction and preserve original transaction currency/amount.

**Business logic / conditions**
- Store timestamps in UTC with explicit source timezone where schedule semantics require it.
- Money uses decimal/atomic representation; never binary floating point for financial values.
- Checkout locks price/currency for a defined validity period.

**Security / privacy**
- Do not allow client-supplied FX rates to determine payable totals.

**UI / UX**
- Locale and currency selectors live in header/profile, not inside every form.
- Right-to-left layout must mirror navigation and directional spacing while preserving ticket/seat semantics.
- Dates and amounts are localized consistently across web, app, PDF ticket and notifications.

### 9. Ticket Domain and Product Template Configuration
**Source baseline:** Requirements define configurable ticket types and product templates across bus/train/movie/events/sports/flight/ferry/attractions.
**Actors:** Tenant Admin, Agent/Shop Owner

**Functional requirements**
- Provide domain templates and dynamic field schemas for each ticket type.
- Allow creation of custom product types by combining capabilities, fields, inventory kind, schedule model and policies.
- Allow draft/save/publish/unpublish/disable lifecycle.

**Business logic / conditions**
- Domain template supplies sensible defaults but never hard-codes core booking flow to one domain.
- Required fields depend on selected capabilities; e.g. seat map requires seat-layout definition, timed service requires schedule.
- Published products cannot remove fields required by active bookings; breaking changes require versioning.

**Acceptance criteria**
- Agent can create bus, train, movie or event service from templates without code changes.

**UI / UX**
- Agent “Add Service” flow begins with visual domain cards, then a stepper: Basics > Schedule/Location > Inventory/Seats > Pricing > Policies > Media > Preview > Publish.
- Show only relevant fields based on domain/capabilities to avoid overwhelming users.

### 10. Catalog and Service Publishing
**Source baseline:** Requirements include catalog, provider products and shop-sold services.
**Actors:** Agent, Customer, Admin

**Functional requirements**
- Catalog stores published services with media, descriptions, location/route/venue, schedules, price-from and seller identity.
- Support publish/unpublish, scheduled publish, pause sales and sold-out status.
- Expose tenant/shared-network discovery according to distribution configuration.

**Business logic / conditions**
- Only valid, active, approved sellers can publish sellable services.
- Changing material details after sales may require customer notification or versioned schedule.
- Unpublished item remains available to admins and historical booking references.

**UI / UX**
- Customer uses browse/search category surfaces rather than a generic “marketplace orders” concept.
- Agent catalog uses status chips: Draft, In Review, Live, Paused, Sold Out, Archived.
- Cards prioritize service name, date/time/location, seller, availability and total-from price.

### 11. Provider Integration and Adapter Framework
**Source baseline:** Blueprint specifies provider adapter contract and WireMock offline stubs.
**Actors:** Integration Engineer, Tenant Admin, Platform Admin

**Functional requirements**
- Adapter supports SEARCH, AVAILABILITY, SEAT_MAP, HOLD, RELEASE, BOOK, CONFIRM, GET_BOOKING, CANCEL, REFUND, MODIFY, TICKET_RETRIEVAL, VALIDATION as applicable.
- Configure endpoint, auth, credentials reference, timeouts, retries, rate limit, circuit breaker, webhooks/polling and reconciliation.
- Provide test connection and provider health visibility.

**Business logic / conditions**
- Core services consume canonical contracts; provider-specific payloads remain behind integration boundary.
- Never retry non-idempotent provider mutations blindly; use idempotency/provider reference and reconciliation.
- Circuit opens on configured failure thresholds and search degrades gracefully.

**Security / privacy**
- Credentials are secret references, never returned to normal UI after save.
- Webhook signatures and replay protection are required where provider supports them.

**Non-functional**
- Search provider calls run in parallel with per-provider timeout budget and partial-result policy.

**UI / UX**
- Admin provider screen has Overview, Capabilities, Credentials, Mapping, Health, Webhooks, Reconciliation and Audit tabs.
- Mapping UI shows provider field → canonical field with transform/validation preview.

### 12. Manual, CSV and Bulk Service Upload
**Source baseline:** Agent manual requires ticket/service upload and no external website dependency.
**Actors:** Agent/Shop Owner, Staff

**Functional requirements**
- Allow manual entry and CSV/template bulk import for services, schedules, inventory and prices.
- Validate upload before commit and show row-level errors.
- Support dry-run preview, partial acceptance policy and downloadable error report.

**Business logic / conditions**
- Bulk operation is idempotent using import batch ID.
- Existing records are matched using configurable external reference or composite key.
- Destructive changes require explicit confirmation and cannot silently remove booked inventory.

**UI / UX**
- Upload wizard: Download Template > Upload > Map Columns > Validate > Review > Import > Result.
- Show counts for valid, warnings, errors, new and updates before commit.

### 13. Universal Search and Discovery
**Source baseline:** Blueprint defines unified search across all products/domains/providers with filters and normalized offers.
**Actors:** Customer, Agent-assisted seller

**Functional requirements**
- Accept origin/destination or location, date/time, passengers/quantity, domain and optional preferences.
- Search local/agent inventory and configured external providers concurrently.
- Normalize into UniversalOffer, deduplicate, filter, sort and rank.
- Support price, time, duration, seller/provider, seat type/class, rating, refundability and accessibility filters where relevant.

**Business logic / conditions**
- Results may be partial when one provider is unavailable; label stale/estimated data and revalidate before hold.
- Search result ranking must be explainable at least by primary score factors.
- Search context is preserved when user opens result and returns.

**Analytics / telemetry**
- Track search requests, zero-result rate, provider contribution, filter use, result clicks and search-to-book conversion.

**Non-functional**
- Target initial results p95 <= 2.5 s under normal provider health; progressively render partial results when possible.

**UI / UX**
- Desktop: large universal search console with domain tabs and clear Origin/Destination/Date/Passengers fields.
- Mobile: prominent omnibox plus horizontal category chips and sticky filter/sort controls.
- Results cards adapt by domain but use common hierarchy: seller/provider, schedule, duration/location, availability, policy badge, total price and CTA.

### 14. AI Conversational Search
**Source baseline:** Requirements define natural-language search with offline-first intent parsing and provider search.
**Actors:** Customer, Agent

**Functional requirements**
- Parse natural-language intent into structured search fields.
- Echo interpreted intent before consequential actions.
- Ask focused clarification only for required ambiguous fields.
- Use normal Universal Search after intent parsing rather than bypassing business rules.

**Business logic / conditions**
- AI never invents live price, seat or availability; all such facts must come from tools/APIs.
- If confidence is low, present parsed fields for user correction.

**Security / privacy**
- Prompt/content is treated as untrusted input; tool calls pass explicit schemas and authorization context.

**UI / UX**
- Chat input supports text and optional voice; parsed chips show From, To, Date, Passengers, Domain.
- Results transition seamlessly to normal search result cards, preserving ability to edit filters.

### 15. Recommendation and Personalization
**Source baseline:** Blueprint defines offline deterministic + ML recommendations.
**Actors:** Customer

**Functional requirements**
- Recommend services based on popularity, context, history, preferences and availability.
- Allow non-personalized fallback for new users.
- Provide user controls for personalization preference.

**Business logic / conditions**
- Recommendations cannot override hard constraints such as availability or unsupported route.
- Sensitive attributes are not used unless explicitly permitted and legally appropriate.

**UI / UX**
- Use “Recommended for you”, “Popular near you” and “Best value” shelves with explanation labels.
- Avoid endless carousel overload; provide See All and clear domain label.

### 16. Smart Multi-Service Trip Planner
**Source baseline:** Requirements specify cross-domain itinerary and bookable options.
**Actors:** Customer

**Functional requirements**
- Accept origin/destination, dates, budget, travelers and preferences.
- Generate ordered legs across transport/activities and surface feasibility.
- Allow user to replace/remove legs and recalculate.
- Support creation of Trip with multiple booking references after purchase.

**Business logic / conditions**
- Connections require configurable minimum transfer buffer.
- Planner must detect impossible overlaps and time-zone transitions.
- Prices/availability are revalidated before checkout.

**UI / UX**
- Use timeline itinerary UI with each leg as card, connection buffers, warnings and running total.
- AI planner has clear “Suggested” label and editable constraints panel.

### 17. Inventory Models and Availability
**Source baseline:** Blueprint specifies seat, capacity, general admission and provider-synchronized inventory.
**Actors:** Agent, Customer, Inventory Service

**Functional requirements**
- Support seat-level, capacity-level, general admission and externally-owned inventory.
- Expose real-time availability and inventory status.
- Support inventory adjustments with reason and audit.

**Business logic / conditions**
- Inventory cannot fall below confirmed sold quantity.
- Externally-owned inventory is authoritative at provider and locally cached only within policy.
- Manual oversell override, if allowed, is privileged and clearly audited.

**UI / UX**
- Agent inventory screen offers calendar/list mode plus seat/capacity view.
- Use color-coded status: available, held, sold, blocked, unavailable; always pair color with label/icon.

### 18. Seat Map Designer and Seat Selection
**Source baseline:** Requirements include configurable seat maps and AI seat recommendation.
**Actors:** Agent Designer, Customer

**Functional requirements**
- Create reusable seat-map layouts with sections/coaches/screens/rows/seats and seat categories.
- Allow seats to be blocked, accessible, premium, couple/group or provider-specific categories.
- Customer selects seats with live availability and price updates.

**Business logic / conditions**
- Seat selection is tentative until hold succeeds.
- Group seating recommendation prefers adjacency but does not split unless user accepts.
- Seat map version used by an active schedule cannot change incompatibly without migration.

**Non-functional**
- Seat availability update should propagate within seconds in managed real-time inventory.

**UI / UX**
- Customer seat map uses zoom/pan, legend, price categories and accessible list fallback.
- Agent designer uses grid/canvas with bulk row creation and property inspector.
- Mobile has bottom sheet showing selected seats and running total.

### 19. Reservation Hold and Concurrency
**Source baseline:** Blueprint state machine AVAILABLE → HELD → CONFIRMED; HELD → EXPIRED → AVAILABLE.
**Actors:** Customer, Booking/Inventory Services

**Functional requirements**
- Create temporary hold for selected inventory before payment/booking finalization.
- Expose hold expiry timestamp and remaining time.
- Release on explicit cancel, timeout or compensated failure.
- Prevent double-sell under concurrency.

**Business logic / conditions**
- Use optimistic locking/versioning for owned inventory; distributed coordination only where needed.
- Hold duration is configurable by domain/provider and cannot exceed provider hold.
- Expired hold cannot be confirmed; checkout must re-hold/revalidate.

**Security / privacy**
- Hold endpoint must be idempotent and bound to authenticated session/user/booking intent.

**Non-functional**
- Concurrency tests must prove no oversell under target load.

**UI / UX**
- Checkout shows visible non-alarming reservation timer.
- On expiry, preserve form data and provide “Check availability again” rather than destructive reset.

### 20. Universal Booking Engine
**Source baseline:** Blueprint defines offer → hold → payment → confirm → issue ticket with compensation.
**Actors:** Customer, Agent-assisted seller

**Functional requirements**
- Create booking from validated offer/selection.
- Maintain booking items across one or multiple services where supported.
- Coordinate hold, payment, provider confirmation and ticket issuance.
- Provide booking reference and immutable customer-facing summary.

**Business logic / conditions**
- Booking uses saga/orchestration with explicit compensating actions.
- A retry with same idempotency key returns same logical outcome.
- Partial multi-leg failure must surface clear remediation: rollback/refund or split completion according to product policy.

**Analytics / telemetry**
- Track checkout step conversion, payment abandon, confirmation time and failure reason.

**Non-functional**
- Booking core target p95 <= 3 s excluding external provider/payment latency; asynchronous operations expose progress safely.

**UI / UX**
- Checkout stepper: Review > Travelers/Attendees > Seats/Add-ons > Payment > Confirmation.
- Always show seller/provider, cancellation policy and final total before pay.
- Confirmation page emphasizes booking reference, ticket access and next action rather than upsells.

### 21. Agent-Assisted / Counter Sales
**Source baseline:** Requirements explicitly support customer self-service and agent-facilitated purchase.
**Actors:** Agent Staff, Customer

**Functional requirements**
- Allow agent to search/select service, create customer or guest, choose seats, record payment method and issue ticket.
- Support walk-in cash recording if tenant enables it.
- Allow agent to resend ticket by email/SMS/social channel.

**Business logic / conditions**
- Cash sale requires branch/counter permissions and reconciliation entry.
- Agent cannot bypass price/policy unless privileged override policy exists and reason is captured.

**UI / UX**
- Fast POS layout optimized for keyboard/tablet: search left/top, service results center, cart/customer summary right.
- Use large touch targets and quick passenger reuse for counter efficiency.

### 22. Payments and Payment Gateway Abstraction
**Source baseline:** Requirements support CARD/WALLET/PAYPAL/BANK and pluggable gateways with sandbox/mocks.
**Actors:** Customer, Agent, Finance Ops

**Functional requirements**
- Initiate, authorize/settle, query and reconcile payment.
- Support multiple gateways per country/currency with configurable routing/failover.
- Use tokenized/hosted payment components when practical to minimize PCI scope.
- Handle asynchronous webhooks and status polling.

**Business logic / conditions**
- Payment callback is idempotent and signature-verified.
- Client success screen never treats client redirect alone as settlement truth.
- Currency and amount are locked from server-side booking price.

**Security / privacy**
- Never store raw CVV; minimize PAN storage through tokenization.
- Webhook endpoints implement signature, timestamp and replay protection.

**Non-functional**
- Payment status reconciliation job detects stuck/pending transactions.

**UI / UX**
- Checkout displays available methods based on tenant/country/currency.
- Show fees before selection when payment-method fee applies.
- Pending payment has dedicated state with refresh/status and support path.

### 23. Settlement, Commission and Payout
**Source baseline:** Production scope includes commission, settlement ledger and payout cycles.
**Actors:** Agent Owner, Tenant Finance, Platform Finance

**Functional requirements**
- Calculate platform fee, tenant fee, agent/provider commission, taxes and payable balance.
- Maintain immutable settlement ledger independent of mutable booking display state.
- Support payout cycles, holds, adjustments and downloadable statements.

**Business logic / conditions**
- Every financial adjustment has reason, actor and references.
- Refunds/chargebacks reverse appropriate ledger entries rather than editing history.
- Payout destination change requires step-up auth and optional cooling period.

**Security / privacy**
- Financial exports and payout details require least-privilege access and masking.

**UI / UX**
- Agent finance dashboard: Available, Pending, Paid, Refunds, Fees; settlement table with period/status/export.
- Platform finance view supports tenant/shop drill-down and reconciliation exceptions.

### 24. Ticket Issuance, QR/Barcode and Wallet
**Source baseline:** Requirements specify deterministic QR/barcode ticket and validation endpoint.
**Actors:** Customer, Agent, Gate/Validator

**Functional requirements**
- Issue digital ticket only after booking confirmation policy is satisfied.
- Generate QR/barcode containing tamper-evident reference/token, not unnecessary PII.
- Support PDF/download, in-app wallet and resend/share.
- Track ticket status: ISSUED, USED/VALIDATED, CANCELLED, VOID, EXPIRED as domain permits.

**Business logic / conditions**
- Validation is atomic where online and protects against reuse.
- Offline validation uses signed payload and later sync when domain risk allows.
- Cancelled/refunded tickets are invalid even if old image remains.

**Security / privacy**
- QR payload signed/encrypted as appropriate and avoids sequential guessable booking IDs.

**UI / UX**
- Ticket wallet card shows service, date/time, seat, status and QR reveal.
- QR screen boosts brightness and minimizes surrounding UI.
- Agent can print compact ticket/receipt format where configured.

### 25. Ticket Validation / Gate Operations
**Source baseline:** Blueprint includes validation and offline validation capability.
**Actors:** Validator Staff, Agent Staff

**Functional requirements**
- Scan QR/barcode and return Valid, Already Used, Cancelled, Expired, Wrong Event/Service or Unknown.
- Support manual code entry fallback.
- Record validator device/staff/location and timestamp.

**Business logic / conditions**
- Validation scope is restricted to assigned event/service/venue where applicable.
- Offline device sync resolves duplicates with server truth and flags conflicts.

**UI / UX**
- Validator app is extremely simple: full-screen scanner, large result color + icon + text, optional attendee/seat details.
- Success auto-resets scanner quickly; errors require acknowledgement when operationally important.

### 26. Cancellation, Reschedule and Refund
**Source baseline:** Requirements define configurable cancellation policies, partial refunds and provider rules.
**Actors:** Customer, Agent, Support/Finance

**Functional requirements**
- Calculate eligibility and estimated refund before confirmation.
- Support full/partial cancellation and provider-specific constraints.
- Track refund states and reconcile payment/provider outcomes.
- Support reschedule/modify when capability exists.

**Business logic / conditions**
- Policy evaluation uses booked version of policy where legally/contractually required.
- Cancellation does not complete until provider confirmation when provider is authoritative.
- Refund amount cannot exceed captured refundable amount.

**Security / privacy**
- High-value/manual refunds can require step-up approval and maker-checker workflow.

**UI / UX**
- My Booking shows “Manage booking” with eligible actions; unavailable actions show explanation rather than disappearing when useful.
- Cancellation confirmation clearly shows refund amount, fees, method and expected timeline.

### 27. Pricing, Fare, Tax and Fee Engine
**Source baseline:** Blueprint defines fixed/tiered/time/demand/promotional/AI-recommended modes and separate fare components.
**Actors:** Agent, Tenant Admin, Customer

**Functional requirements**
- Calculate base fare, taxes, service fees, add-ons, discount, commission-visible internal breakdown and customer total.
- Support price rules by domain, route/location, schedule, seat/category, customer type and time window.
- Return price quote with expiry/version.

**Business logic / conditions**
- Rule precedence and stacking are deterministic and documented.
- Tax/fee rounding occurs at defined component/order level per jurisdiction configuration.
- AI price recommendation is advisory unless tenant explicitly enables automated bounded application.

**UI / UX**
- Admin/Agent pricing builder uses rule cards with IF conditions and THEN action plus priority and date range.
- Customer sees transparent total and material mandatory fees before final payment.

### 28. Promotions, Vouchers and Offers
**Source baseline:** Requirements cover percentage/flat/voucher/offer, scoped promotions and single-use rules.
**Actors:** Customer, Agent Marketing, Tenant Admin

**Functional requirements**
- Create promotion code/rule with type, value, min purchase, dates, max uses, domain/product scope and eligibility.
- Validate and apply during pricing/checkout.
- Track redemption and campaign performance.

**Business logic / conditions**
- Expired, exhausted, disabled or ineligible promotion returns safe reason.
- Promotion stacking policy is explicit: exclusive, stackable group or best-of.
- Redemption is finalized only with successful booking/payment according to policy.

**UI / UX**
- Customer promo field is optional and non-blocking; applied discount appears immediately in breakdown.
- Marketing UI includes campaign status, usage, revenue influenced and conversion.

### 29. Loyalty, Wallet Credits and Membership
**Source baseline:** Requirements include loyalty accounts, points, levels, redemption and wallet/credits.
**Actors:** Customer, Tenant Admin

**Functional requirements**
- Earn points based on configurable event (booking completed, travel completed, campaign bonus).
- Support tiers/memberships and redemption at checkout where enabled.
- Maintain ledger of accrual, redemption, expiration and adjustment.

**Business logic / conditions**
- Points are ledger-based, not directly mutable balance.
- Refund can reverse earned points and restore redeemed points according to policy.

**UI / UX**
- Customer Loyalty page shows balance, tier progress, benefits and activity.
- Checkout shows redeem option only when eligible and communicates value clearly.

### 30. Reviews and Ratings
**Source baseline:** API/docs include product reviews and averages.
**Actors:** Customer, Agent, Moderator

**Functional requirements**
- Allow eligible customers to rate/review a purchased/attended product.
- Show aggregate rating and review list.
- Support moderation/reporting and seller response where enabled.

**Business logic / conditions**
- Only verified purchase can receive “Verified booking” badge.
- Prevent duplicate review for same booking item unless edit policy allows.

**UI / UX**
- Review form uses rating, optional title/comment and clear content guidelines.
- Agent sees review trends and unresolved responses.

### 31. Notifications and Communication Preferences
**Source baseline:** Requirements include notifications, per-user settings and omnichannel messaging.
**Actors:** Customer, Agent, Admin

**Functional requirements**
- Generate transactional notifications for booking, payment, ticket, cancellation, refund, schedule changes and support.
- Respect channel preferences for non-mandatory marketing notifications.
- Support in-app notification center plus pluggable email/SMS/push/social channels.

**Business logic / conditions**
- Critical transactional messages may bypass marketing opt-out but must comply with legal/tenant policy.
- Deduplicate repeated event notifications using event/message key.

**UI / UX**
- Notification center groups unread/read with domain icon and deep link to related booking.
- Settings separate Transactional, Service Updates and Marketing preferences.

### 32. Omnichannel Messaging: WhatsApp, Facebook and Social
**Source baseline:** Requirements specify inbound webhook, outbound messaging and WhatsApp/Facebook/Telegram/SMS adapters.
**Actors:** Customer, Agent, Support

**Functional requirements**
- Connect tenant messaging channels through credentials/configuration.
- Receive inbound webhook and associate conversation with tenant/customer where possible.
- Send booking links, status, ticket delivery and support messages.
- Maintain conversation log and consent state.

**Business logic / conditions**
- All channels use the same canonical booking/customer services; channel is presentation/transport only.
- Unsupported free-form actions in social chat redirect to secure web/app for payment or sensitive identity steps.

**Security / privacy**
- Verify webhook authenticity and redact secrets/PII in conversation analytics.

**UI / UX**
- Agent conversation inbox shows channel badge, customer, unread count, last message and linked booking.
- Quick replies/templates are tenant-configurable and localized.

### 33. Customer Profile, Saved Travelers and Consent
**Source baseline:** Requirements include profile, saved passengers, preferences, consent and PII protection.
**Actors:** Customer

**Functional requirements**
- Manage personal profile, saved traveler/passenger profiles, contact methods and consents.
- Allow deletion/export request surfaces according to privacy policy and jurisdiction configuration.
- Mask sensitive fields in read views.

**Business logic / conditions**
- Customer owns saved traveler data or must have authority/consent to store it.
- Deleting a saved traveler does not alter historical booking records required for legal/accounting retention.

**UI / UX**
- Profile uses separate sections for Personal Details, Travelers, Security, Preferences, Privacy and Connected Services.

### 34. Family Groups
**Source baseline:** Requirements explicitly include create/join/remove family groups.
**Actors:** Customer/Family Organizer

**Functional requirements**
- Create family group, invite/join members and remove members.
- Allow optional shared traveler management or booking visibility only when explicitly configured/consented.

**Business logic / conditions**
- Family membership does not implicitly grant access to payment methods or sensitive booking details.
- Minor/dependent handling is policy/jurisdiction-specific and requires explicit design before production use.

**UI / UX**
- Family page shows group members, role, invitation status and shared capabilities clearly.

### 35. User Settings and Preferences
**Source baseline:** Requirements include theme/language/currency/notifications settings.
**Actors:** All users

**Functional requirements**
- Persist language, currency, theme, notification and accessibility preferences.
- Synchronize preference across devices after sign-in.

**Business logic / conditions**
- Tenant-enforced policies override user preference when necessary but UI explains restriction.

**UI / UX**
- Settings use grouped rows with immediate-save controls where safe and explicit Save for multi-field forms.

### 36. Referrals and Invite Friends
**Source baseline:** Requirements include referral code, invitation and validation.
**Actors:** Customer

**Functional requirements**
- Generate referral code/link, send invitation and show status.
- Validate code at registration/eligible transaction.
- Apply reward only after configured qualifying event.

**Business logic / conditions**
- Prevent self-referral and abuse using identity/device/payment/risk signals.
- Referral reward is ledger/event driven and reversible for fraudulent/refunded qualifying activity.

**UI / UX**
- Referral page shows link/code, share actions, reward terms and invited status.

### 37. My Trips, Orders and Booking History
**Source baseline:** Requirements include trips, orders, bookings and tickets; customer can self-manage.
**Actors:** Customer

**Functional requirements**
- Provide unified history for upcoming, completed, cancelled and refunded bookings/orders.
- Group multi-leg bookings into Trips.
- Deep-link from order to payment, ticket and support.

**Business logic / conditions**
- History remains available according to retention policy even if product is unpublished.
- Status is derived from authoritative booking/payment/ticket states, not client cache alone.

**UI / UX**
- Mobile uses tabbed Upcoming / Completed / Cancelled with compact ticket cards.
- Desktop supports filters by date/domain/status and export/receipt where enabled.

### 38. Support and 24/7 Service Desk
**Source baseline:** Requirements include support tickets, SLA, 24/7 operations and manuals.
**Actors:** Customer, Agent, Support Agent, Admin

**Functional requirements**
- Create support case with category, priority, subject, description and related booking/payment.
- Generate request reference and SLA due time.
- Support assignment, status, internal notes, customer replies and resolution.

**Business logic / conditions**
- Priority/SLA can be rule-driven by category, customer tier and incident severity.
- Sensitive support actions use scoped permissions and are audited.

**Analytics / telemetry**
- Track first response, resolution time, reopen rate and SLA breach.

**UI / UX**
- Customer Support center offers FAQs/search then case creation; never block case creation behind failed bot answers.
- Agent/Admin console uses queue table with SLA countdown, filters and linked customer/booking timeline.

### 39. Agent Dashboard and Analytics
**Source baseline:** Agent manual requires dashboard, monitoring, sales and service management.
**Actors:** Agent Owner/Manager

**Functional requirements**
- Show bookings, sales, active services, customers, inventory alerts, refunds and payout snapshot.
- Provide trend charts and top services/routes.
- Expose operational alerts requiring action.

**Business logic / conditions**
- Metrics respect selected shop/branch and timezone/date range.
- Financial metrics distinguish gross sales, refunds, fees and net settlement.

**UI / UX**
- Follow the premium light dashboard reference: dark left nav, white cards, indigo accent, restrained charts, green/red status used only semantically.
- Top KPI row followed by sales trend, top services and recent bookings; avoid more than 4-6 primary KPIs above fold.

### 40. Platform Admin Dashboard and Global Operations
**Source baseline:** Requirements define global tenants/providers/agents/users monitoring and system health.
**Actors:** Platform Admin, Operations

**Functional requirements**
- Show GMV, bookings, active agents/shops, users, tenants and service health.
- Monitor provider/payment failure, fraud alerts, support SLA and operational incidents.
- Drill into tenant/shop without exposing unnecessary PII.

**Business logic / conditions**
- Global metrics have explicit currency aggregation strategy; never sum mixed currencies without normalization.
- Health status comes from observability data and service checks, not manual flags.

**UI / UX**
- Admin uses dense enterprise layout with left rail and global search.
- System Health card shows service name, status, latency/error and last check.
- Critical alerts appear as an action queue, not decorative dashboard tiles.

### 41. Moderation and Marketplace Governance
**Source baseline:** Requirements include INSTANT/REVIEW moderation and shop approve/suspend.
**Actors:** Platform/Tenant Admin

**Functional requirements**
- Configure moderation mode per tenant and possibly per seller risk tier.
- Review shop applications, product/service reports and policy violations.
- Suspend/reinstate shop with reason and effective time.

**Business logic / conditions**
- Suspension blocks new sales but preserves customer rights/ticket access.
- High-risk enforcement actions require reason and audit; optionally maker-checker.

**UI / UX**
- Moderation queue uses status filters, risk badges, submitted documents summary and clear decision actions.

### 42. Fraud, Abuse and Cyber Risk Engine
**Source baseline:** Blueprint and APIs include fraud checks, anomaly scoring and admin fraud operations.
**Actors:** Security/Fraud Ops, Booking/Payment services

**Functional requirements**
- Calculate risk using booking velocity, amount, quantity, attempts, cards/devices/IP/account behavior and configurable signals.
- Return score, risk level, flags and action recommendation.
- Support ALLOW, CHALLENGE/REVIEW and BLOCK outcomes.

**Business logic / conditions**
- Rules and ML score are versioned and explainable to internal operators.
- Fraud engine cannot silently cancel a confirmed booking without a defined operational process.
- False-positive handling and manual override require audit.

**Non-functional**
- Fraud decision latency target suitable for checkout, e.g. p95 <= 300 ms for local scoring.

**UI / UX**
- Security console shows alert queue, risk score, reasons, related entities and timeline.
- Never expose internal fraud thresholds to normal customers; customer receives safe verification/error message.

### 43. Disruption Detection and Recovery
**Source baseline:** Requirements define disruption report and recovery recommendations.
**Actors:** Operations, Customer, Agent

**Functional requirements**
- Detect provider outage, schedule delay/cancellation, inventory mismatch and service disruption.
- Identify affected bookings and recommend rebooking/refund/notification action.
- Track incident status and recovery.

**Business logic / conditions**
- Automatic customer-impacting actions are policy-controlled; recommendations may require operator confirmation.
- Alternative service must be revalidated before offer/rebooking.

**UI / UX**
- Operations incident view combines provider health, affected bookings, recommended actions and communication status.
- Customer sees concise disruption banner with next steps, not raw infrastructure error.

### 44. Business Analytics and Ticket Intelligence
**Source baseline:** Blueprint defines revenue, bookings, conversion, inventory utilization and provider performance dashboards.
**Actors:** Agent, Tenant Admin, Platform Admin

**Functional requirements**
- Provide standardized metrics dictionary and dashboards by role.
- Support filters for date, tenant/shop, domain, provider, product, route/location and currency.
- Expose provider SLA/latency/error and business conversion metrics.

**Business logic / conditions**
- Metric definitions are centralized in semantic layer to avoid dashboard disagreement.
- Financial aggregation converts with documented rate/date and preserves source currency for drill-down.

**UI / UX**
- Use KPI cards, trend charts, ranked bars and tables; avoid pie charts for high-cardinality data.
- Every chart includes timeframe, unit, tooltip and empty/loading/error states.

### 45. AI Data Analyst
**Source baseline:** Blueprint specifies NL question → semantic layer → validated SQL → read-only warehouse → analysis.
**Actors:** Agent Manager, Tenant Admin, Platform Analyst

**Functional requirements**
- Accept natural-language analytics question and map to approved metrics/dimensions.
- Generate SQL only against allowlisted read-only analytical views.
- Validate query, execute with limits, return answer, supporting table/chart and assumptions.
- Offer follow-up questions based on same analytical context.

**Business logic / conditions**
- Never execute arbitrary DDL/DML or unrestricted production SQL.
- If metric ambiguity exists, analyst asks clarification or states chosen definition.
- Result must cite data freshness and filters used.

**Security / privacy**
- Read-only credentials, row-level tenant security, query timeout/row limits and audit of prompts/queries.

**UI / UX**
- Chat-style analyst with generated chart/table panel and “View query logic” safe explanation for power users.
- Provide suggested prompts by role such as “Why did cancellations increase this week?”.

### 46. Data Platform, Kafka and Analytical Storage
**Source baseline:** Requirements define Kafka, event ingest, Parquet/DuckDB and cloud storage.
**Actors:** Data Engineer, Platform Services

**Functional requirements**
- Publish business events through transactional outbox/event pattern.
- Stream into analytical ingestion with schema/version metadata.
- Support local Parquet/DuckDB analytics and cloud S3/warehouse evolution.
- Track processed/pending ingest state.

**Business logic / conditions**
- Events are immutable facts; corrections use new events.
- Schema compatibility policy applies to producer changes.
- Consumer processing is idempotent and supports DLQ/replay.

**UI / UX**
- No end-user UI required; Ops/Admin exposes event lag, DLQ count, ingest errors and data freshness.

### 47. Event-Driven Domain Architecture
**Source baseline:** Blueprint lists booking/payment/ticket/inventory/provider/fraud events.
**Actors:** Backend Services, Operations

**Functional requirements**
- Publish events such as ReservationHeld, ReservationExpired, BookingCreated/Confirmed/Cancelled, PaymentCompleted, RefundCompleted, TicketIssued, InventoryChanged, ProviderFailed and FraudAlert.
- Include correlation ID, tenant ID, event ID, type, version and occurredAt.

**Business logic / conditions**
- Consumers cannot assume global ordering; partition keys are chosen by aggregate where ordering matters.
- At-least-once delivery requires idempotent consumers.

**Non-functional**
- Monitor consumer lag, retry, DLQ and schema errors.

**UI / UX**
- Developer/ops event viewer may filter by correlation ID, booking, tenant and event type.

### 48. API Gateway, Edge, Rate Limiting and API Versioning
**Source baseline:** Blueprint defines API gateway/edge, health, rate limits and metrics.
**Actors:** All clients, API Partners, Platform Ops

**Functional requirements**
- Centralize external routing, authentication enforcement, rate limiting, correlation IDs, request size limits and API version headers/path.
- Provide public, authenticated, partner and admin policy tiers.
- Expose stable error model and documentation.

**Business logic / conditions**
- Gateway does not contain core business logic.
- Rate limits vary by identity/client/IP/endpoint sensitivity and return standard retry hints.
- Breaking API change requires version strategy and deprecation period.

**Security / privacy**
- Protect admin/private routes and prevent trusting spoofable tenant headers from public clients.

**UI / UX**
- No primary UI; developer portal/API docs show base URLs, scopes, examples, errors, rate limits and changelog.

### 49. API Partner Keys and Machine-to-Machine Access
**Source baseline:** Requirements mention API partners/app keys/security profile.
**Actors:** Tenant Admin, API Partner

**Functional requirements**
- Create client/app credentials with scopes, environment and expiration/rotation policy.
- Display secret once and allow revoke/rotate.
- Support audit and usage metrics by app/client.

**Business logic / conditions**
- No permanent broad-scope default keys.
- Credential rotation can overlap old/new key for controlled migration.

**Security / privacy**
- Secrets stored hashed or in secret manager according to credential type; never retrievable after creation.

**UI / UX**
- Integration settings page shows app name, client ID, scopes, created/last used/status with Create, Rotate, Revoke actions.

### 50. Role-Based and Attribute-Based Authorization
**Source baseline:** Requirements define Customer/Agent/Admin and tenant/branch/resource scoping.
**Actors:** All users/services

**Functional requirements**
- Implement RBAC role templates plus contextual ABAC for tenant, organization, shop, branch, ownership and resource status.
- Enforce server side at every protected action.
- Provide permissions matrix to admins.

**Business logic / conditions**
- UI hiding is convenience only and never replaces server authorization.
- Deny by default for new privileged capability until explicitly assigned.

**UI / UX**
- Role editor groups permissions by domain and shows affected scope.
- Dangerous permissions have warning and may require MFA.

### 51. PII Encryption, Privacy and Data Lifecycle
**Source baseline:** Security/privacy guide specifies masked PII and production hardening.
**Actors:** All users, Security/Privacy Admin

**Functional requirements**
- Classify PII and sensitive financial/identity data.
- Encrypt sensitive data at rest and in transit.
- Mask values in UI/logs and expose only minimum required fields.
- Support retention, deletion/anonymization and export workflows as policy requires.

**Business logic / conditions**
- Retention is driven by tenant/jurisdiction/record type and legal hold.
- Historical financial/legal records may be retained while profile data is deleted/anonymized as permitted.

**Security / privacy**
- No PII in tracing labels, metrics dimensions or URLs.

**UI / UX**
- Privacy center explains stored categories, consents, export/delete requests and status.

### 52. AI Agent Runtime and Tool Authorization
**Source baseline:** Blueprint defines controlled AI tools, least privilege and confirmation for financial actions.
**Actors:** Customer, Agent, Admin via AI

**Functional requirements**
- Register tools with schema, required permissions, risk category and confirmation policy.
- Authorize every tool call against the real user/service context.
- Audit prompt, selected tool, arguments summary, result status and model/runtime.
- Require explicit user confirmation for booking/payment/refund or other consequential action.

**Business logic / conditions**
- LLM output is never trusted as authorization.
- Tool results are authoritative for live state; model cannot fabricate success.
- High-risk admin tools should not be available to customer-facing agent.

**Security / privacy**
- Prompt injection defense, tool allowlists, argument validation, output validation and data minimization.

**UI / UX**
- Agent UI shows proposed action card before confirmation, including price/policy and “Confirm”/“Cancel”.
- Expose tool progress and failure recovery in plain language.

### 53. AI Guardrails, Prompt Security and Evaluation
**Source baseline:** Requirements include LLM guardrails, prompt security and ML evaluation.
**Actors:** AI Platform, Security

**Functional requirements**
- Implement input/output safety checks, prompt injection detection, retrieval filtering and tool policy enforcement.
- Maintain evaluation suites for intent accuracy, tool selection, hallucination/live-fact grounding, policy compliance and SQL correctness.
- Version prompts/models/policies and compare regression before production promotion.

**Business logic / conditions**
- Unsafe or low-confidence output falls back to deterministic UX or human support where appropriate.
- PII and secrets are redacted/minimized before third-party model calls when configured.

**UI / UX**
- Admin AI evaluation dashboard shows dataset, version, pass rate, failure categories, latency and cost.

### 54. Demand Forecasting, Price Prediction and Dynamic Pricing
**Source baseline:** Blueprint defines forecast, price projection and bounded dynamic pricing with guardrail.
**Actors:** Agent/Revenue Manager, Customer for advisory indicators

**Functional requirements**
- Forecast demand by product/date horizon and expose confidence.
- Project price trend and sell-out risk as advisory customer insight.
- Calculate dynamic price recommendation using demand/capacity signals with configured min/max multiplier.

**Business logic / conditions**
- Prediction is not a guarantee and must show confidence/context.
- Automated pricing cannot exceed configured regulatory/commercial guardrails.
- Model/rule version used for a quoted price is recorded.

**UI / UX**
- Agent revenue page shows forecast chart, demand heatmap and recommended price with Apply/Ignore.
- Customer language says “Price may rise” or similar probabilistic wording, never certainty.

### 55. Observability, Logging, Tracing and Metrics
**Source baseline:** Requirements mandate OpenTelemetry, Jaeger, Prometheus, Grafana, Micrometer and structured logging.
**Actors:** Platform Ops, Developers, Security

**Functional requirements**
- Instrument HTTP, Kafka, provider, database and key business flows with distributed tracing.
- Expose technical and business metrics.
- Use structured logs with traceId, spanId, correlationId, tenantId and safe business references.

**Business logic / conditions**
- Never log secrets, raw tokens, card data or unnecessary PII.
- Correlation ID propagates across synchronous and asynchronous boundaries.

**Non-functional**
- Define SLOs and alerts based on user-impacting symptoms, not only CPU thresholds.

**UI / UX**
- Grafana dashboards separate platform overview, service health, provider integrations, booking/payment funnel and security.
- Admin-facing system health is simplified; engineering dashboards retain technical detail.

### 56. Operational Health, Incident and Auto-Recovery
**Source baseline:** Ops manual includes health endpoints, auto-detection, auto-fix and incident log.
**Actors:** Operations/SRE

**Functional requirements**
- Provide liveness/readiness and dependency health.
- Detect stuck jobs, provider outage, consumer lag, payment pending and elevated error rate.
- Automate safe remediation such as restart/resume/retry where idempotent and approved.
- Maintain incident log and runbooks.

**Business logic / conditions**
- Auto-fix must be bounded, observable and escalate after repeated failure.
- Never auto-retry financial/provider mutation without idempotency/reconciliation safety.

**UI / UX**
- Ops console uses incidents timeline, severity, impacted tenants/services, current mitigation and owner.

### 57. Backup, Restore and Disaster Recovery
**Source baseline:** Deployment/runbook docs include backups and recovery.
**Actors:** SRE, Database/Platform Admin

**Functional requirements**
- Define backups for relational DB, object storage/configuration and required secrets/metadata.
- Test restore regularly and document RPO/RTO by deployment tier.
- Support tenant-aware recovery strategy where feasible.

**Business logic / conditions**
- Backup success is not considered sufficient without restore testing.
- Secrets/key recovery procedures must preserve ability to decrypt historical protected data.

**UI / UX**
- Ops dashboard shows latest backup, last restore test, age, status and retention.

### 58. Deployment Portability: Windows, Linux, Docker, Kubernetes and Cloud
**Source baseline:** Self-host guide requires Windows/Linux, Docker, Kubernetes/EKS, AWS and generic VM/cloud.
**Actors:** Customer IT, SRE, DevOps

**Functional requirements**
- Provide environment profiles for local, single VM, Docker Compose, Kubernetes/EKS and customer-managed cloud/on-prem.
- Externalize configuration and secrets.
- Provide migrations, health verification and upgrade/rollback instructions.

**Business logic / conditions**
- Application behavior must not depend on local filesystem persistence except configured object/storage abstractions.
- Production profiles disallow insecure dev defaults.

**UI / UX**
- Deployment docs include copyable commands, configuration matrix, topology diagrams and verification checklist.

### 59. Kubernetes, GitOps and Autoscaling
**Source baseline:** Requirements specify Helm, Argo CD, Argo Rollouts, KEDA, HPA/VPA and Network Policies.
**Actors:** DevOps/SRE

**Functional requirements**
- Package services via Helm and reconcile desired state with Argo CD.
- Use progressive delivery through Argo Rollouts.
- Scale stateless services via HPA; event consumers via KEDA; apply VPA cautiously.
- Apply default-deny Network Policies and resource requests/limits.

**Business logic / conditions**
- Autoscaling signals match workload: CPU/latency for APIs, lag for Kafka consumers.
- Production rollout automatically rolls back on configured health/SLO gates.

**Non-functional**
- No single replica for critical production service where HA tier requires redundancy.

**UI / UX**
- Argo/Grafana dashboards provide rollout status, replica health, lag and resource saturation.

### 60. AWS Reference Architecture and Infrastructure as Code
**Source baseline:** Requirements specify AWS, Terraform, EKS, Lambda, RDS, DynamoDB, Route53, S3, CloudWatch.
**Actors:** Cloud/DevOps

**Functional requirements**
- Provision repeatable infrastructure with Terraform.
- Use EKS for platform runtime, RDS for PostgreSQL, S3 for object/data, Route53 for DNS, CloudWatch for AWS-native integration and optional Lambda/DynamoDB for justified use cases.
- Separate environments/accounts or equivalent isolation according to deployment tier.

**Business logic / conditions**
- Cloud-specific services remain behind abstractions when portability matters.
- Terraform state is protected, locked and access-controlled.

**Security / privacy**
- Use least-privilege IAM, encryption/KMS and private networking for data services.

**UI / UX**
- No end-user UI; provide cloud architecture diagrams, environment matrix and deployment verification dashboards/documentation.

### 61. CI/CD and DevSecOps Pipeline
**Source baseline:** Requirements define GitHub Actions with tests/scans/SBOM/GitOps/DAST/progressive rollout.
**Actors:** Developers, DevSecOps, Release Manager

**Functional requirements**
- Pipeline stages: lint/compile, unit/architecture tests, dependency/secret/IaC scans, SBOM, build image, container scan, integration/contract/e2e/performance, publish, GitOps staging, DAST, approval, progressive production rollout, smoke and rollback.
- Use Trivy, Grype, Syft, Gitleaks, Checkov and OWASP ZAP as specified.
- Publish signed/versioned artifacts and release metadata.

**Business logic / conditions**
- Failed mandatory security/quality gate blocks promotion unless documented emergency process applies.
- Secrets never enter workflow logs.
- Production deployment comes from immutable artifact already tested in lower environment.

**UI / UX**
- GitHub Actions summary should expose test counts, coverage, vulnerabilities by severity, SBOM artifact, image digest and deployment link.

### 62. Testing Strategy and Quality Gates
**Source baseline:** Requirements specify JUnit, Mockito, ArchUnit, REST Assured, Testcontainers, Pact, Playwright, k6 and JMH.
**Actors:** Engineering, QA

**Functional requirements**
- Maintain unit, architecture, integration, contract, API, E2E, performance and security tests.
- Use Testcontainers for realistic dependencies and WireMock for unavailable external providers.
- Run customer, agent and admin critical journeys in Playwright.

**Business logic / conditions**
- Critical concurrency/financial/security paths require regression tests before release.
- Flaky tests are quarantined only temporarily with owner and due date, not silently ignored.

**UI / UX**
- QA reports present pass/fail by capability and environment plus known risks.

### 63. WireMock, Provider Simulators and Offline-First Development
**Source baseline:** Requirements explicitly prefer free/offline APIs and WireMock/provider simulators.
**Actors:** Developers, QA, Integrators

**Functional requirements**
- Provide deterministic mock bus/train/movie/event providers and payment gateways.
- Simulate search, availability, seat map, hold, booking, cancellation, refund, webhook, timeout, rate-limit and malformed/error responses.
- Use same canonical adapter contracts as live integration.

**Business logic / conditions**
- Test fixtures are versioned and reproducible.
- Mocks must not mask contract drift; Pact/contract checks validate integration schemas.

**UI / UX**
- Developer docs provide scenario names and endpoints; optional simulator console can trigger delays/failures/webhooks.

### 64. Performance, Scalability and Reliability
**Source baseline:** Scope requires production global system with autoscaling and high security.
**Actors:** Platform Engineering

**Functional requirements**
- Define load profiles for search, booking, seat hold, payment callbacks, validation and analytics.
- Set SLOs for availability, latency, error rate and data freshness.
- Use caching, async processing and horizontal scaling where safe.

**Business logic / conditions**
- Never cache mutable availability beyond configured freshness without clear revalidation.
- Backpressure and queue limits prevent one tenant/provider from exhausting platform resources.

**Non-functional**
- Search p95 <= 2.5 s target under healthy providers; auth <= 500 ms; local fraud <= 300 ms; exact final SLOs must be load-tested and deployment-tier-specific.
- Critical write APIs are idempotent where retried.
- Graceful degradation preserves ticket access/status when recommendation/analytics systems are unavailable.

**UI / UX**
- Loading states use skeletons/progress and partial results rather than frozen screens.

### 65. Accessibility and Inclusive UX
**Source baseline:** Global production UX requires accessible web/mobile design; existing docs do not fully specify this, so this is production elaboration.
**Actors:** All users

**Functional requirements**
- Target WCAG 2.2 AA for web experiences.
- Provide keyboard navigation, visible focus, semantic labels, screen-reader names and accessible form errors.
- Do not encode state using color alone.
- Seat map has an accessible list alternative.

**Business logic / conditions**
- Localization/RTL and zoom/text scaling are tested with accessibility, not treated separately.

**UI / UX**
- Minimum comfortable touch targets on mobile, clear contrast, restrained motion, reduced-motion support and readable typography.

### 66. Responsive Design System and Look & Feel
**Source baseline:** Uploaded UI/UX image establishes premium white + indigo/purple product direction across Customer, Agent and Admin.
**Actors:** All channels

**Functional requirements**
- Create shared design tokens for color, typography, spacing, radius, elevation, icon size and breakpoints.
- Support tenant theme overrides within safe token schema.
- Maintain reusable components for buttons, fields, tabs, cards, tables, dialogs, drawers, charts, nav and status badges.

**Business logic / conditions**
- Customer UI is spacious and discovery-led; Agent/Admin UI is denser and operations-led.
- White-label changes brand tokens/content, not core usability or security states.

**UI / UX**
- Primary palette: deep indigo/purple brand accent on white/soft-neutral surfaces; dark navy navigation for Agent/Admin where appropriate.
- Use 8px-based spacing rhythm, 10-16px card radius and subtle shadows/borders.
- Typography hierarchy: large confident hero, clear 20-32px section headings, 14-16px body, 12-13px metadata.
- Keep primary action indigo/purple; success green, warning amber, destructive red only for semantic states.
- Customer mobile uses bottom navigation; desktop customer uses top navigation. Agent/Admin use left rail + top context bar.
- Dark mode supported through tokens, not independent duplicated layouts.

### 67. Customer Web/PWA Information Architecture
**Source baseline:** Frontend strategy requires responsive React web/PWA customer experience.
**Actors:** Customer

**Functional requirements**
- Primary navigation: Home, Explore/Search, My Trips, Offers/Loyalty, Support, Profile.
- Home combines universal search, categories, relevant offers, recent/continue booking and recommendations.
- Provide responsive PWA behavior and installability if enabled.

**Business logic / conditions**
- Authenticated state changes header from Sign In to avatar/profile without rearranging primary navigation dramatically.

**UI / UX**
- Desktop reference uses full-width hero/search, service cards and contextual side modules.
- Avoid admin-style dense tables in customer experience; use cards/timelines.

### 68. Customer Mobile Application UX
**Source baseline:** Scope requires customer app and screenshot defines mobile patterns.
**Actors:** Customer

**Functional requirements**
- Provide Home, Search, My Trips/Tickets, Offers/Wallet and Profile tabs.
- Support push notifications, ticket wallet, QR display and deep links.
- Preserve search/checkout state through app backgrounding.

**Business logic / conditions**
- Critical ticket display remains available offline after secure local cache where permitted.

**UI / UX**
- Use 390px-class mobile reference with prominent search, category chips, continue-booking card, trending cards and bottom nav.
- Booking result and confirmation screens use sticky bottom CTA and concise top app bar.

### 69. Agent Web and Agent Mobile UX
**Source baseline:** Scope explicitly requires complete agent web/app, not only analytics.
**Actors:** Agent/Shop Owner/Staff

**Functional requirements**
- Agent Web supports full operational workflows; Agent Mobile supports onboarding, add service, inventory, assisted sales, scan/issue/share ticket and alerts.
- Keep feature parity for critical shop operation while allowing complex configuration to be easier on web.

**Business logic / conditions**
- Permissions and branch scope determine visible actions.

**UI / UX**
- Agent Web: dark/navy left rail, light workspace, KPI cards and operational tables.
- Agent Mobile: dashboard summary, quick actions Add Service/Bookings/Inventory/Reports, recent bookings and service wizard.
- Never call primary agent surface “Marketplace”; use Shop, Services, Sales/Bookings and Customers.

### 70. Platform Admin UX
**Source baseline:** Scope requires global platform control, monitoring and moderation.
**Actors:** Platform Admin/Ops/Security

**Functional requirements**
- Provide Overview, Tenants, Agents/Shops, Users, Providers, Transactions, Disputes/Support, Security, Reports, Settings and System Logs.
- Use role-specific admin home for Operations vs Security where appropriate.

**Business logic / conditions**
- Admin actions with broad impact display scope, consequence and confirmation.

**UI / UX**
- Enterprise dense layout with left navigation, top global search and contextual filters.
- Use tables for manageable entity lists and cards/charts for summaries; drawer/detail panel for quick inspection, full page for complex workflows.

### 71. Release Management, SDLC and Definition of Done
**Source baseline:** SDLC docs define planning, build, test, release, operate and DoD.
**Actors:** Engineering, QA, Product, Operations

**Functional requirements**
- Maintain requirements traceability, architecture decisions, tests, security evidence, release notes and runbooks.
- Version APIs, DB migrations and deployment manifests.
- Definition of Done includes code, tests, security, observability, docs, deployment and failure handling.

**Business logic / conditions**
- No production release when critical known issue lacks explicit risk acceptance.
- Rollback path is validated before release.

**UI / UX**
- Release dashboard/checklist can be represented in GitHub/CI rather than a bespoke product UI.
