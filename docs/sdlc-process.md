# TicketMesh — SDLC Process

> How TicketMesh is planned, built, tested, released and operated — as a standard company.

## 1. Process Overview

TicketMesh follows a lightweight, iterative SDLC with a strong quality and automation
focus. Each change flows through a repeatable pipeline.

```
Plan -> Design -> Build -> Test -> Review -> Release -> Operate -> Observe/Learn
```

## 2. Planning

- Requirements are captured in the master register (`docs/spec/REQUIREMENTS.md`) — the
  single source of truth.
- Each feature is broken into small, shippable units with clear Definition of Done.

## 3. Build

- Backend: Java 21 / Spring Boot 3.3.5 / Maven. Explicit constructors (no Lombok).
- Tests are written alongside code (test-first where practical).
- Conventions: explicit `@PathVariable`/`@RequestParam`, standalone MockMvc, surefire JVM
  args for byte-buddy, `mock-maker-inline`.

## 4. Test & Quality

- **Unit tests** per domain service (booking, payment, ticket, support, fraud, AI).
- **Integration tests** using H2 (MySQL mode) + MockMvc for full flows.
- **Contract/offline tests** with WireMock for provider and external API calls.
- All tests use real assertions; no placeholder tests.
- Quality gate: `mvn test` must pass with **0 failures** before merge/release.

## 5. Release

- Versioned, tagged releases with **development release notes** and **QA release notes**
  (see `docs/release-notes-dev.md`, `docs/release-notes-qa.md`).
- CI builds and runs the test gate on every push and pull request (see `.github/workflows`).
- Articles promoted from dev -> QA -> production environments.

## 6. Operate & Automate (live + dev + QA)

- **Health checks**: liveness `/api/health/live`, readiness, actuator health.
- **Auto-detect issues**: monitors + health probes detect problems in live/dev/QA.
- **Auto-fix**: scripted remediation (safe, reversible) addresses detected issues where
  possible; otherwise the ops runbook is triggered.
- **Fraud detection**: automated risk scoring auto-blocks suspicious transactions and flags
  them for the 24/7 support team.
- **24/7 support**: support portal with tickets, priority/SLA, assignment, escalation and
  automated SLA-breach escalation.
- **Observability**: metrics + logs + health feed the ops dashboard and runbook.

## 7. Definition of Done

- Code builds and all tests pass.
- Docs updated (requirements, release notes, manuals, API).
- No secrets or real customer data in the repository.
- Committed by the authorised author.
