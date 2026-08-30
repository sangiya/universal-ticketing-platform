# TicketMesh — Self-Host / Deployment Guide

> How to deploy TicketMesh yourself on **Windows**, **Linux**, **Docker / docker-compose**,
> **Kubernetes / EKS (Helm)**, or **any cloud** (AWS ECS via Terraform, or a generic VPS).
> Use this to run the `ticketmesh-core` backend plus optional frontend, as licensed
> software, self-hosted, or as a fully managed service. For autoscaling specifics see
> `docs/deployment-autoscaling.md`; for per-tenant white-label see
> `docs/white-label-guide.md`.

## 1. Prerequisites

- **Java 21** (Temurin or equivalent LTS JVM).
- **Maven 3.9+** (only needed to build from source).
- **MySQL 8** runtime database (the app also runs its tests on H2 in MySQL mode).
- Optional: **Docker**, **kubectl** + a cluster, `helm`, or `terraform` for the relevant
  target.

Verify:

```bash
java -version        # 21+
mvn -version         # 3.9+
```

## 2. Environment configuration

All configuration is via environment variables (secrets are never committed). The table
below lists the important ones and their defaults (from `application.yml`).

| Variable | Default | Purpose |
|----------|---------|---------|
| `DB_URL` | `jdbc:mysql://localhost:3306/ticketmesh_db?...` | JDBC connection string |
| `DB_USERNAME` | `ticketmesh_app` | DB user |
| `DB_PASSWORD` | `ticketmesh_password` | DB password |
| `JWT_SECRET` | `change-this-super-secret-key-please-2026-ticketmesh` | Signs auth JWTs |
| `QR_SECRET` | `change-this-qr-signing-secret-please-2026` | Signs QR ticket payloads (HMAC-SHA256) |
| `PII_MASTER_KEY` | `ticketmesh-pii-master-key-change-me-2026!!` | AES-256/GCM encryption key for PII at rest |
| `WHATSAPP_ENABLED` | `true` | Enable real WhatsApp/messaging transport (vs offline stub) |
| `WHATSAPP_ENDPOINT` | `https://offline.ticketmesh.local/whatsapp/send` | WhatsApp outbound endpoint |
| `BOOTSTRAP_ADMIN_USERNAME` | `admin` | Initial admin username |
| `BOOTSTRAP_ADMIN_PASSWORD` | `ChangeMe123!` | Initial admin password |
| `JWT_EXPIRATION_MS` | `86400000` | JWT lifetime (ms) |
| `RESERVATION_PENDING_TIMEOUT_MS` / `HOLD_TIMEOUT_MS` | `900000` / `1800000` | Reservation hold windows |
| `SUPPORT_SLA_CHECK_MS` | `60000` | Support SLA sweep interval |

> **Security hardening first:** change `JWT_SECRET`, `QR_SECRET`, `PII_MASTER_KEY` and the
> bootstrap admin password before any real traffic. Generate strong random values, e.g.
> `openssl rand -base64 48`. In production prefer a secrets manager (AWS Secrets Manager,
> HashiCorp Vault, Kubernetes secrets) referenced from these env vars.

## 3. Build

```bash
mvn -B -DskipTests package      # artifact: target/ticketmesh-core-*.jar
```

Run the full test suite before deploying:

```bash
mvn test                        # 135 tests, 0 failures, BUILD SUCCESS
```

## 4. Deploy on Windows (bare metal)

1. Install Java 21 and MySQL 8.
2. Create a database and user:

```sql
CREATE DATABASE ticketmesh_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'ticketmesh_app'@'localhost' IDENTIFIED BY 'ticketmesh_password';
GRANT ALL PRIVILEGES ON ticketmesh_db.* TO 'ticketmesh_app'@'localhost';
```

3. Set environment variables (PowerShell):

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/ticketmesh_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:DB_USERNAME="ticketmesh_app"
$env:DB_PASSWORD="ticketmesh_password"
$env:JWT_SECRET="<strong-random>"
$env:QR_SECRET="<strong-random>"
$env:PII_MASTER_KEY="<strong-random>"
$env:BOOTSTRAP_ADMIN_PASSWORD="<strong-admin-password>"
```

4. Run (Flyway runs migrations automatically):

```powershell
java -jar target/ticketmesh-core-*.jar
```

The API is at `http://localhost:8080`, health at `http://localhost:8080/actuator/health`.

Use `--server.port=8080` or a reverse proxy (IIS ARR / nginx) to serve on 80/443 with TLS.

## 5. Deploy on Linux (bare metal / VPS)

1. Install Java 21 and MySQL 8, create the DB as above.
2. Put the jar at, e.g., `/opt/ticketmesh/app.jar`.

3. Create a systemd unit `/etc/systemd/system/ticketmesh.service`:

```ini
[Unit]
Description=TicketMesh core
After=network.target mysql.service

[Service]
User=ticketmesh
WorkingDirectory=/opt/ticketmesh
ExecStart=/usr/bin/java -Xms512m -Xmx1g -jar /opt/ticketmesh/app.jar
Environment=DB_URL=jdbc:mysql://localhost:3306/ticketmesh_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
Environment=DB_USERNAME=ticketmesh_app
Environment=DB_PASSWORD=CHANGE_ME
Environment=JWT_SECRET=CHANGE_ME
Environment=QR_SECRET=CHANGE_ME
Environment=PII_MASTER_KEY=CHANGE_ME
Environment=BOOTSTRAP_ADMIN_PASSWORD=CHANGE_ME
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

4. Enable and start:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now ticketmesh
sudo systemctl status ticketmesh
```

Point a reverse proxy (nginx / Caddy) at `127.0.0.1:8080` with TLS termination.

## 6. Docker / docker-compose

A `Dockerfile` (multi-stage, non-root) and `docker-compose.yml` (MySQL + app) are included.

Run the full stack:

```bash
docker compose up -d --build
```

Verify:

```bash
curl -s http://localhost:8080/actuator/health
```

Override secret env vars for production by editing the `app` service environment in
`docker-compose.yml` (or via a `.env` file). The compose stack wires the app to the `mysql`
service over the internal network.

## 7. Kubernetes / EKS (Helm)

A Helm chart is included at `infra/helm/ticketmesh`.

```bash
helm upgrade --install ticketmesh infra/helm/ticketmesh \
  --set secrets.dbPassword="CHANGE_ME" \
  --set secrets.jwtSecret="CHANGE_ME" \
  --set secrets.qrSecret="CHANGE_ME" \
  --set secrets.piiMasterKey="CHANGE_ME" \
  --set bootstrap.adminPassword="CHANGE_ME"
```

The chart ships a Deployment (with liveness/readiness probes), a Service, an HPA
(CPU/memory autoscaling), a ConfigMap and a Secrets object. See the chart values for full
options. The bundled `infra/kubernetes/*` manifests are equivalent for direct `kubectl`
use.

## 8. Any cloud

### AWS ECS (Terraform)

`infra/terraform` provisions ECS Fargate with target-tracking service auto scaling,
CloudWatch logs and least-privilege IAM. Provide the same secret env vars (or point at
Secrets Manager) and a managed RDS MySQL, then `terraform apply`.

### Generic VPS / cloud VM

Install as in the Linux section (§5) on any provider (AWS EC2, DigitalOcean, Azure VM,
GCP, etc.). Use the provider's managed MySQL or self-host, and front with their load
balancer.

## 9. Horizontal autoscaling

The backend is **stateless** — all state lives in the database and outbox tables — so it
scales horizontally:

- **Kubernetes**: the bundled HPA scales on CPU/memory (see `infra/kubernetes/hpa.yaml`).
- **AWS ECS**: target-tracking auto scaling (see `infra/terraform`).
- Ensure the load balancer uses `/actuator/health` for the target health check.

## 10. Backups & recovery

- **Database**: schedule `mysqldump` (or use the provider's managed-PITR) regularly; keep
  the schema versioned by Flyway so restores match the app version.
- **Secrets**: keep `JWT_SECRET`, `QR_SECRET`, `PII_MASTER_KEY` **backed up and stable** —
  you cannot decrypt PII or verify QR/tickets after a key rotation unless planned.
- **Config**: the repository is the source of truth; migrations are versioned (Flyway).

## 11. Frontend (optional)

Build the consumer PWA + admin/agent portals and serve the static bundle:

```bash
cd frontend && npm install && npm run build   # outputs to dist/
```

Serve `dist/` from any static host/CDN, pointing to your backend base URL. The PWA is
installable once served over HTTPS.
