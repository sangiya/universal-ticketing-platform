# TicketMesh — Deployment & Autoscaling Guide

> How to deploy TicketMesh to **AWS, Kubernetes (EKS), on-premises, Docker and a single
> server**, with horizontal autoscaling and easy, repeatable deployments.

## 1. Build

```
mvn -DskipTests clean package
```

Produces `target/ticketmesh-core-1.0.0.jar` (Spring Boot fat jar). The app is stateless —
all state lives in the database — so it scales horizontally by running more instances.

## 2. Configuration (environment variables)

| Variable | Default | Purpose |
|----------|---------|---------|
| `DB_URL` | `jdbc:mysql://localhost:3306/ticketmesh_db` | JDBC URL |
| `DB_USERNAME` | `ticketmesh_app` | DB user |
| `DB_PASSWORD` | `ticketmesh_password` | DB password (override in prod!) |
| `JWT_SECRET` | (dev default) | Override in production |
| `QR_SECRET` | (dev default) | Override in production |
| `BOOTSTRAP_ADMIN_USERNAME` | `admin` | Bootstrap admin |
| `BOOTSTRAP_ADMIN_PASSWORD` | `ChangeMe123!` | **Change in production** |
| `SUPPORT_SLA_CHECK_MS` | `60000` | Support SLA sweep interval |
| `PROVIDER_CONNECT_TIMEOUT_MS` | `2000` | Provider client connect timeout |
| `PROVIDER_READ_TIMEOUT_MS` | `3000` | Provider client read timeout |

Secrets must come from an environment/secrets manager, never from the repository.

## 3. Container

A Dockerfile builds the image. The app exposes `8080`. Use a read-only root and a
non-root user for security.

## 4. Target deployments

### 4.1 Docker (single server / simple)

```
docker build -t ticketmesh-core .
docker run -d -p 8080:8080 --env-file .env ticketmesh-core
```

### 4.2 Kubernetes / EKS

- **Deployment** with health checks: liveness `/api/health/live`, readiness `/actuator/health`.
- **Horizontal autoscaling** with HPA (CPU/memory) + optionally KEDA (queue/rate based).
- **Ingress** terminates TLS; exposes `/api/**`.
- See `infra/kubernetes/` and `infra/helm/` for manifests.

Autoscaling config (concept):
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata: { name: ticketmesh-core }
spec:
  scaleTargetRef: { apiVersion: apps/v1, kind: Deployment, name: ticketmesh-core }
  minReplicas: 2
  maxReplicas: 20
  metrics:
    - type: Resource
      resource: { name: cpu, target: { type: Utilization, averageUtilization: 60 } }
```

### 4.3 AWS (non-K8s)

- ECS with a Service Auto Scaling target (min 2 / max N, scaling on CPU/mem/ALB request
  count) or EC2 ASG with an ALB.
- Managed database (RDS/Aurora) for reliability + backups.
- See `infra/terraform/` for Terraform modules.

### 4.4 On-premises

- Deployment + service + HPA/own load balancer; or simple service manager (systemd/Docker)
  on a single server with a reverse proxy.

## 5. Database & migrations

- Flyway manages schema migrations (versioned, applied on startup).
- Use a managed/replicated DB in production; point it at `DB_URL`.

## 6. Health & readiness

- `GET /api/health/live` — liveness for process restarts.
- `GET /actuator/health` — readiness incl. DB connectivity (fail fast on bad config).

## 7. Scaling guidance

- App horizontal scaling: safe (stateless).
- DB scaling: choose instance size / read replicas to match load; keep connection pool
  sized to instance count.
- Slot reservations / inventory: configured timeouts + reconciliation keep held inventory
  from leaking when instances are recycled.

## 8. Verification after deploy

1. Hit `/actuator/health` -> `{"status":"UP"}`.
2. Hit `/api/health/live` -> `{"status":"UP"}`.
3. Smoke login + catalog search + booking flow (see test API doc examples).
4. Confirm metrics flowing to Prometheus/Grafana and alerts active.
