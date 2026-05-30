# GPU-Hub

GPU-Hub is a platform for managing GPU cluster resource allocation across teams, projects, and workloads. It provides a single control plane on top of one or more Kubernetes clusters, letting administrators register clusters, define resource quotas, organize teams and projects, and let users submit GPU workloads (Jupyter notebooks and LLM inference servers) that are scheduled fairly through a queue hierarchy.

## Overview

GPU-Hub integrates several systems into a cohesive resource-management workflow:

- **Kubernetes** (via the Fabric8 client) — multi-cluster support; kubeconfigs are stored in MinIO and clients are created on demand per cluster.
- **KAI Scheduler** — provides the queue hierarchy (team-level queues → project-level child queues) used to enforce GPU quotas and fair sharing.
- **MinIO / S3** — stores cluster kubeconfigs and acts as the source for data-set migration into team storage.
- **JuiceFS** — backs one shared `ReadWriteMany` PVC per team-cluster; data sources are mounted as subfolders.
- **PostgreSQL** — persistence for all domain entities.

### Core concepts

| Concept | Description |
|---------|-------------|
| **Cluster** | A registered Kubernetes cluster (kubeconfig validated and stored in MinIO). |
| **Policy** | Resource quota/limit definitions (GPU, CPU, memory) plus an optional node pool, scoped per cluster. |
| **Team** | A group of users with membership roles (`MEMBER`, `TEAM_LEAD`); assigned to clusters with a per-team storage PVC. |
| **Project** | A team-owned Kubernetes namespace on a cluster under a policy. |
| **Workload** | A submitted job — a Jupyter `Notebook` (Kubeflow CRD) or an `LLM_INFERENCE` deployment (vLLM). |
| **Data Source** | An S3 bucket copied into a folder inside the team PVC, mountable into workloads. |

### Tech stack

- **Backend** — Spring Boot 4 / Java 21 (Maven), Fabric8 Kubernetes client, MinIO SDK, JWT auth, PostgreSQL.
- **Frontend** — React 19 + TypeScript, Vite, TanStack Query, Axios, Tailwind CSS v4, shadcn/ui (base-ui primitives).

## Repository layout

```
src/
├── backend/          # Spring Boot 4 / Java 21 API (Maven) — primary codebase
├── frontend/         # React 19 + Vite single-page application
└── infrastructure/   # Kubernetes manifests (GPU operator, KAI Scheduler, Kubeflow, MinIO, vLLM, etc.)
```

## Prerequisites

Make sure the following are installed and available:

- **Java 21** (JDK)
- **Node.js 20+** and **npm**
- **PostgreSQL 14+** — a database named `gpu-hub`
- **MinIO** (or any S3-compatible object store)
- **Docker** (optional — for building/running container images)
- One or more **Kubernetes clusters** with KAI Scheduler installed (required only to actually run workloads; the API and UI boot without them)

The schema is created automatically on backend startup (via `spring.sql.init.schema-locations`, currently `schema_v7.sql`), so you only need to create an empty database.

## Backend — setup & run

All backend commands run from `src/backend/`. Always use the Maven wrapper (`./mvnw`), not a system Maven install.

### 1. Start the supporting services

PostgreSQL and MinIO are **not** bundled — point the backend at running instances. Quick start with Docker:

```bash
# PostgreSQL
docker run -d --name gpu-hub-postgres \
  -e POSTGRES_DB=gpu-hub \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=1234 \
  -p 5432:5432 postgres:16

# MinIO
docker run -d --name gpu-hub-minio \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  -p 9000:9000 -p 9001:9000 \
  minio/minio server /data --console-address ":9001"
```

### 2. Configure environment variables

Sensible defaults for local development are baked into [src/backend/src/main/resources/application.yaml](src/backend/src/main/resources/application.yaml), so the backend runs out of the box against the Docker services above. Override these for any other environment:

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/gpu-hub` | PostgreSQL JDBC URL |
| `DB_USERNAME` | `postgres` | DB username |
| `DB_PASSWORD` | `1234` | DB password |
| `JWT_SECRET` | *(dev default)* | Base64 secret used to sign JWTs |
| `MINIO_ENDPOINT` | `http://localhost:9000` | MinIO/S3 endpoint |
| `MINIO_ACCESS_KEY` | `minioadmin` | MinIO access key |
| `MINIO_SECRET_KEY` | `minioadmin` | MinIO secret key |
| `KUBERNETES_KUBECONFIG_BUCKET` | `kubeconfig` | Bucket where cluster kubeconfigs are stored |
| `ADMIN_BOOTSTRAP_USERNAME` | `admin` | Initial admin account created on first boot |
| `ADMIN_BOOTSTRAP_PASSWORD` | `admin` | Initial admin password |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` | Allowed frontend origins |

Optional JuiceFS knobs (`JUICEFS_STORAGE_CLASS`, `JUICEFS_PVC_SIZE`, `JUICEFS_MIGRATION_IMAGE`, `JUICEFS_BACKOFF_LIMIT`) and Kubernetes timeouts (`KUBERNETES_CONNECTION_TIMEOUT_MS`, `KUBERNETES_REQUEST_TIMEOUT_MS`) also have defaults in `application.yaml`.


### 3. Run the API

```bash
cd src/backend
./mvnw spring-boot:run          # starts the API on http://localhost:8080
```

On first boot the schema is created and a bootstrap admin account (`admin` / `admin` by default) is provisioned.

The API listens on **port 8080**. Public endpoints are `/api/auth/**`; everything else requires a `Bearer` JWT.

## Frontend — setup & run

All frontend commands run from `src/frontend/`.

### 1. Install dependencies

```bash
cd src/frontend
npm install
```

### 2. Run the dev server

```bash
npm run dev          # dev server at http://localhost:5173
```

The Vite dev server proxies `/api` → `http://localhost:8080` (see [src/frontend/vite.config.ts](src/frontend/vite.config.ts)), so make sure the backend is running first. Open http://localhost:5173 and log in with the bootstrap admin credentials.

## Running with Docker

There is no Compose orchestration — the images are standalone and ship no PostgreSQL/MinIO container. Point each image at external services via the env vars above. Build contexts matter:

```bash
# from src/
# Full-stack image (SPA baked into the backend JAR's static/) — build context is src/
docker build -f backend/Dockerfile -t gpu-hub:latest .

# Backend-only image (serves only /api/**) — build context is backend/
docker build -f backend/Dockerfile.backend-only -t gpu-hub-backend:latest backend

# Standalone frontend image (Node → Nginx)
docker build -f frontend/Dockerfile -t gpu-hub-frontend:latest frontend
```

Run a backend image against external PostgreSQL and MinIO:

```bash
docker run --rm -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host:5432/gpu-hub -e DB_USERNAME=postgres -e DB_PASSWORD=... \
  -e MINIO_ENDPOINT=http://host:9000 -e MINIO_ACCESS_KEY=... -e MINIO_SECRET_KEY=... \
  -e KUBERNETES_KUBECONFIG_BUCKET=kubeconfig -e JWT_SECRET=... \
  gpu-hub-backend:latest
```

The standalone frontend image takes `FRONTEND_BACKEND_HOST` and `FRONTEND_BACKEND_PORT` env vars to point Nginx at the backend.

## Typical first-run workflow

1. Start PostgreSQL and MinIO.
2. Start the backend (`./mvnw spring-boot:run`) — the schema and the bootstrap admin are created automatically.
3. Start the frontend (`npm run dev`) and log in as `admin` / `admin`.
4. As admin: **register a cluster** (upload its kubeconfig), define a **policy**, create **teams** and assign them to the cluster, then create **projects**.
5. Users can then **submit workloads** (Notebook or LLM inference) and optionally attach **data sources**.

## Further documentation
- [src/infrastructure/](src/infrastructure/) — Kubernetes manifests for the supporting platform components.
</content>
</invoke>
