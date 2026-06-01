# ReporteCiudadanoAdmin

Government admin dashboard for tracking and managing citizen pothole reports submitted via the ReporteCiudadano mobile app.

## Stack

| Layer | Technology |
|---|---|
| Backend | Ktor server (JVM, Netty engine) — module `:backend` |
| Frontend | Compose Multiplatform (wasmJs) — module `:frontend` |
| Auth | AWS Cognito User Pool (PKCE) + Ktor JWT validation |
| Database | AWS DynamoDB — `reporte-ciudadano-reports` (us-east-1) |
| Storage | AWS S3 — `reporte-ciudadano-photos` (us-east-1, read-only) |
| Container | Docker (eclipse-temurin:21) |
| Deployment | ECS Fargate + ALB (backend) / CloudFront + S3 (frontend) |

## Build & Run

```bash
# Run backend locally
./gradlew :backend:run

# Run frontend (dev server)
./gradlew :frontend:wasmJsBrowserDevelopmentRun

# Build fat JAR
./gradlew :backend:shadowJar

# Build frontend for production
./gradlew :frontend:wasmJsBrowserProductionWebpack

# Local full stack (requires ~/.aws credentials)
docker-compose up
```

## Tests

```bash
./gradlew :backend:test
```

## Agents

Specialized agents in `.claude/agents/`:

| Agent | Concern |
|---|---|
| `aws-solutions-architect` | ECS, ALB, Cognito, CloudFront, ECR, IAM provisioning |
| `ktor-developer` | Backend routes, DynamoDB, S3, JWT auth |
| `compose-web-developer` | WASM frontend screens, Leaflet map, API client |
| `devops-engineer` | GitHub Actions CI/CD, Docker, ECS rolling deploys |
| `business-analyst` | User stories for government official workflows |
| `versioning` | Commits, branching, changelog |

## Feature Pipeline

```
Architect → BA → Designer → Developer → QA → Versioning
```

All features tracked in `features.md`. Architecture decisions in `architecture.md`.

## Rules

- Never modify `/ReporteCiudadano/` (the mobile app)
- Never commit credentials — use env vars or ECS task role
- Never make S3 buckets public — backend serves presigned URLs (15-min TTL)
- All routes except `/health` require `Authorization: Bearer <cognito-jwt>`
