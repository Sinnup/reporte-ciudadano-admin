# Architecture — ReporteCiudadanoAdmin

Authored by: AWS Solutions Architect + Senior KMP Engineer | Date: 2026-06-01

---

## Summary

Web-based government admin dashboard. Officials log in via Cognito, view and update pothole reports stored in DynamoDB/S3 by the ReporteCiudadano mobile app. The admin project shares no code with the mobile app; it only shares the same AWS data layer.

---

## AWS Architecture

```
                  ┌─────────────────────────────────────┐
                  │         AWS (us-east-1)             │
                  │                                     │
Browser           │  CloudFront ──► S3                  │
  ──────────────► │  (WASM frontend assets)              │
                  │                                     │
  ─── login ────► │  Cognito User Pool                  │
  ◄── JWT token ─ │  (hosted UI, username/password)     │
                  │                                     │
  ─── API calls ► │  ALB ──► ECS Fargate                │
  (Bearer JWT)    │          (Ktor/Netty, port 8080)    │
                  │               │                     │
                  │               ├──► DynamoDB          │
                  │               │   reporte-ciudadano-reports │
                  │               │                     │
                  │               └──► S3               │
                  │                   reporte-ciudadano-photos  │
                  │                                     │
                  │  ECR (Docker image registry)         │
                  │  CloudWatch (logs)                  │
                  │  Secrets Manager (aws creds)        │
                  └─────────────────────────────────────┘
```

---

## Project Structure

```
ReporteCiudadanoAdmin/          (project root = /Users/sinue/Documents/Reporte Ciudadano)
  backend/                       Ktor server (JVM, Netty engine)
    src/main/kotlin/
      Application.kt             Entry point, Ktor engine config
      plugins/
        Auth.kt                  Cognito JWT validation (ktor-auth-jwt)
        Routing.kt               Route registration
        Serialization.kt         kotlinx.serialization JSON
        CORS.kt                  CORS headers for frontend domain
      routes/
        ReportsRoutes.kt         CRUD + status update
        PhotosRoutes.kt          Pre-signed S3 URL generation
        HealthRoutes.kt          ALB health check
      aws/
        DynamoDbClient.kt        AWS SDK v2 for DynamoDB
        S3Client.kt              AWS SDK v2 for S3 + presigned URLs
      domain/
        Report.kt                Data class (mirrors DynamoDB schema)
        ReportStatus.kt          Enum: SENT SEEN PENDING IN_PROGRESS RESOLVED DISCARDED
    Dockerfile
    build.gradle.kts

  frontend/                      Compose Multiplatform (wasmJs target)
    src/wasmJsMain/kotlin/
      main.kt                    Entry point
      ui/
        screens/
          LoginScreen.kt         Redirects to Cognito hosted UI
          ReportsListScreen.kt   Paginated list with filters
          ReportDetailScreen.kt  Read-only + status update dropdown
          ReportsMapScreen.kt    Leaflet map via JS interop (all pins)
        components/
          StatusBadge.kt
          ReportCard.kt
      network/
        ApiClient.kt             Ktor client calling backend REST
        AuthStore.kt             JWT token storage (sessionStorage)
      domain/
        Report.kt                Shared data model
        ReportStatus.kt
    build.gradle.kts

  .github/workflows/
    backend.yml                  Build → Docker → push ECR → deploy ECS
    frontend.yml                 Build WASM → deploy to S3 → invalidate CloudFront

  .claude/agents/                Claude Code specialist agents (shared with team)
  memory/                        AI session context persistence
  scripts/
    setup-local.sh               Patches placeholders with values from local.properties
  local.properties.example       Template — committed, no values
  local.properties               Actual env values — gitignored, never commit
  docker-compose.yml             Local development (Ktor + mock data)
  settings.gradle.kts            Multi-project: includes :backend :frontend
  build.gradle.kts               Root Gradle config
  libs.versions.toml             Version catalog
```

---

## Backend API (Ktor)

Base path: `/api`

| Method | Path | Description |
|---|---|---|
| `GET` | `/health` | ALB health check — always 200 |
| `GET` | `/api/reports` | List all reports (DynamoDB Scan). Query params: `status`, `limit`, `lastKey` (pagination) |
| `GET` | `/api/reports/{id}` | Get single report by ID |
| `PUT` | `/api/reports/{id}/status` | Update report status. Body: `{"status":"SEEN"}` |
| `GET` | `/api/reports/{id}/photos` | List photo keys for a report |
| `GET` | `/api/reports/{id}/photos/{filename}/url` | Generate 15-minute pre-signed S3 GET URL |

All routes except `/health` require `Authorization: Bearer <cognito-jwt>`.

---

## Authentication (Cognito)

- **User Pool**: `reporte-ciudadano-admin-pool` (ID: `COGNITO_USER_POOL_ID` from `local.properties`)
- **App client**: `reporte-ciudadano-admin-web` (no client secret, public PKCE flow; ID: `COGNITO_CLIENT_ID`)
- **Hosted UI domain**: `COGNITO_DOMAIN` from `local.properties` / GitHub Secrets
- **Scopes**: `phone openid email`
- **Token flow**: Authorization Code + PKCE
  1. Frontend reads `COGNITO_DOMAIN` + `COGNITO_CLIENT_ID` from `window.__ENV__` (injected into `index.html` at deploy time)
  2. User clicks "Sign in" → redirected to Cognito Hosted UI
  3. Official logs in; Cognito redirects back with `?code=`
  4. `main.kt` exchanges the code for an access token via `POST /oauth2/token`
  5. Token stored in `sessionStorage` via `AuthStore`
  6. All backend requests carry `Authorization: Bearer <access-token>`
  7. Ktor `ktor-auth-jwt` validates the token against Cognito's JWKS endpoint

### Runtime config injection

`index.html` ships with placeholders; actual values are injected at two points:
- **Local dev**: `scripts/setup-local.sh` reads `local.properties` and patches files in place
- **CI/CD**: `cd-frontend.yml` runs `sed` on the built `index.html` before S3 upload using GitHub Actions secrets (`COGNITO_DOMAIN`, `COGNITO_CLIENT_ID`) stored in the `production` environment

---

## IAM (ECS Task Role)

**Role name**: `reporte-ciudadano-admin-task-role`

ECS tasks use this role via the instance metadata service — no long-lived credentials in the container.

```json
{
  "Statement": [
    {
      "Sid": "DynamoDBAdminAccess",
      "Effect": "Allow",
      "Action": [
        "dynamodb:Scan",
        "dynamodb:GetItem",
        "dynamodb:UpdateItem",
        "dynamodb:DescribeTable"
      ],
      "Resource": "arn:aws:dynamodb:us-east-1:<AWS_ACCOUNT_ID>:table/reporte-ciudadano-reports"
    },
    {
      "Sid": "S3PhotoRead",
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:HeadObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::reporte-ciudadano-photos",
        "arn:aws:s3:::reporte-ciudadano-photos/*"
      ]
    }
  ]
}
```

---

## Domain Models

### `Report` (backend + frontend share the same field names)

| Field | Type | Source |
|---|---|---|
| `id` | String | DynamoDB `id` |
| `title` | String | DynamoDB `title` |
| `description` | String | DynamoDB `description` |
| `latitude` | Double | DynamoDB `latitude` |
| `longitude` | Double | DynamoDB `longitude` |
| `status` | ReportStatus | DynamoDB `status` |
| `createdAt` | Long | DynamoDB `createdAt` (epoch ms) |
| `photoKeys` | List\<String\> | Derived — S3 `ListObjects` with prefix `reports/<id>/` |

### `ReportStatus`

```
SENT        → submitted by citizen, not yet seen
SEEN        → official has opened the report
PENDING     → captured/logged in work system
IN_PROGRESS → repair underway
RESOLVED    → pothole fixed
DISCARDED   → invalid/duplicate
```

---

## Tech Stack (libraries)

### Backend

| Concern | Library | Version |
|---|---|---|
| Server | `ktor-server-netty` | 3.x |
| Auth | `ktor-server-auth-jwt` | 3.x |
| Serialization | `ktor-serialization-kotlinx-json` | 3.x |
| CORS | `ktor-server-cors` | 3.x |
| AWS DynamoDB | `aws-sdk-kotlin` (DynamoDB) | 1.x |
| AWS S3 | `aws-sdk-kotlin` (S3) | 1.x |
| Logging | `logback-classic` | 1.4.x |
| Container | Docker (eclipse-temurin:21-jdk-alpine base) | — |

### Frontend

| Concern | Library |
|---|---|
| UI | Compose Multiplatform (wasmJs) |
| Networking | Ktor client (JS engine) |
| Serialization | kotlinx.serialization |
| DI | Koin |
| Map | Leaflet.js via JS interop |

---

## CI/CD

```
Push to main
  ├─ backend.yml
  │    1. ./gradlew :backend:test
  │    2. ./gradlew :backend:buildFatJar
  │    3. docker build + push to ECR
  │    4. aws ecs update-service (rolling deploy)
  │
  └─ frontend.yml
       1. ./gradlew :frontend:wasmJsBrowserProductionWebpack
       2. aws s3 sync dist/ s3://<frontend-bucket>/
       3. aws cloudfront create-invalidation
```

---

## Feature Sequence

| # | ID | Feature | Branch |
|---|---|---|---|
| 1 | FEAT-001 | Project scaffolding & Gradle setup | `feature/feat-001-scaffold` |
| 2 | FEAT-002 | Gradle 9 upgrade | `feature/feat-002-gradle-9-upgrade` |
| 3 | FEAT-003 | Backend API (DynamoDB + S3 read) | `feature/feat-003-backend-api` |
| 4 | FEAT-004 | Cognito auth (backend JWT validation) | `feature/feat-004-cognito-auth` |
| 5 | FEAT-005 | Reports list screen (frontend) | `feature/feat-005-reports-list` |
| 6 | FEAT-006 | Report detail + status update | `feature/feat-006-report-detail` |
| 7 | FEAT-007 | Reports map view | `feature/feat-007-map` |
| 8 | FEAT-008 | AWS infra provisioning (ECS, ALB, Cognito, CloudFront) | `feature/feat-008-aws-infra` |
| 9 | FEAT-009 | CI/CD pipelines | `feature/feat-009-cicd` |
