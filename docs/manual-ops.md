# TicketMesh — Operations & Runbook

> Runbook for operating TicketMesh in production (AWS / EKS / on-prem / Docker),
> including health checks, monitoring, auto-detection, auto-fix and incident response.

## 1. Health endpoints

- **Liveness**: `GET /api/health/live` — returns `{"status":"UP",...}`.
- **Readiness**: Spring Actuator `GET /actuator/health` — full component health.
- **Metrics**: `/actuator/metrics`, Prometheus format via `/actuator/prometheus` (if exposed).
- **Info**: `/actuator/info`.

Health endpoints are public by design (`/api/health/**`, `/actuator/health/**`) so load
balancers and orchestrators can probe them.

## 2. Monitoring & observability

- Collect logs and metrics (Micrometer/Actuator) into Prometheus + Grafana.
- Alert on:
  - Liveness/readiness `DOWN` (repeated).
  - Support SLA breach rate increasing (auto-escalation firing often).
  - Fraud high-risk count spikes or blocking rate.
  - Booking/payment error rate and latency thresholds.
  - Pod/HPA scaling events (K8s) or ASG (AWS).

## 3. Auto-detection of issues (live + dev + QA)

Automated checks detect problems and raise them automatically:
- **Health probs** (liveness/readiness) for app + database connectivity.
- **SLA sweeps** in support (auto-escalate overdue tickets).
- **Fraud scoring** auto-detects and blocks suspicious transactions.
- **Build/CI** failure detection in dev/QA pipelines gating promotion.

## 4. Auto-fix

For known/safe issues, scripted remediation runs automatically:
- Restart failed/unhealthy instances via orchestrator (K8s liveness probe / AWS health).
- Interrupt long-running reservation holds via the reservation scheduler.
- Re-run failed Flyway migrations if flagged safe; otherwise block startup (fail fast).
- Clear corrupted in-memory state by recycling instances.

Anything not safely auto-fixable escalates to the runbook + support portal.

## 5. Fraud incident response

1. Fraud signal(s) detected and transaction blocked automatically.
2. Support/admin notified via the fraud signals + high-risk counter.
3. Verify the signal (flags, score, actor). If a genuine customer: **admin override** allows
   the transaction and clears the block.
4. If genuine fraud: keep blocked, record audit, suspend involved agent/customer if needed.

## 6. Security

- Secrets only via environment variables / secrets manager — never in the repository.
- Default bootstrap admin password must be changed in production.
- Apply least-privilege RBAC; review audit logs.

## 7. Backup / restore

- Database: scheduled backups + point-in-time restore (managed DB).
- Config: repository is the source of truth; migrations are versioned (Flyway).

## 8. Incident log

Record every incident: time, detection channel (auto/monitor), impact, root cause,
auto-fix vs manual action, resolution, and follow-up prevention item.

## 9. Ops & audit endpoints

Authenticated operations/admin endpoints for live monitoring and investigation:

- **Event feed**: `GET /api/ops/events?limit=10` — recent domain/outbox events
  (booking placed, payment completed, promotion redeemed, disruption raised, ...).
  Use it to trace business activity without querying the DB directly.
- **Disruption report**: `GET /api/ops/disruption?tenantId=` — returns a
  `DisruptionReport` with `activeDisruptions`, `findings` and `recoveryRecommendations`.
  Check this during incidents to get the platform's own assessment and suggested recovery
  actions.
- **Audit log**: `GET /api/audit?tenantId=&limit=` — recent audit-log entries per tenant.
  Review after any incident or suspected unauthorized action.

Pair these with the health/metrics surface (§1-2) for a complete operational picture:
health tells you the app is up; `/ops/events` + `/ops/disruption` + `/audit` tell you what
is actually happening and how to recover.
