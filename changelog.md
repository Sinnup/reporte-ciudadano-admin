# Changelog — ReporteCiudadanoAdmin

All notable changes documented here. Updated per completed feature before merging to `main`.

Format: `[version] — YYYY-MM-DD`

Versioning: Semantic Versioning (`MAJOR.MINOR.PATCH`)
- `MAJOR`: breaking change
- `MINOR`: new backward-compatible feature
- `PATCH`: backward-compatible bug fix

---

## [Unreleased]

---

## [0.6.0] — 2026-06-01

### Added — FEAT-008 AWS CDK Infrastructure Stack

- `infra/bin/app.ts` — CDK app entry point; instantiates `ReporteCiudadanoAdminStack` pinned to account `literal:<AWS_ACCOUNT_ID>` / `us-east-1`
- `infra/lib/reporte-ciudadano-admin-stack.ts` — single CDK stack provisioning:
  - **ECR** — `reporte-ciudadano-admin-backend` repository with imageScanOnPush and 10-image lifecycle rule
  - **VPC** — 2-AZ, public + private subnets, 1 NAT gateway
  - **ECS Fargate cluster** — `reporte-ciudadano-admin`, Container Insights enabled
  - **Task IAM roles** — `reporte-ciudadano-admin-task-role` (DynamoDB + S3 scoped) and execution role (ECR pull + SSM read)
  - **Fargate task definition** — 512 CPU / 1024 MB, port 8080, health check, `COGNITO_USER_POOL_ID` + `COGNITO_CLIENT_ID` sourced from SSM Parameter Store
  - **ALB** — internet-facing, HTTP → HTTPS redirect, HTTPS listener with ACM cert from `acmCertArn` context, health check `/health`
  - **S3 bucket** — `reporte-ciudadano-admin-frontend` (private, block all public, versioning on, SSE-S3, enforce SSL)
  - **CloudFront distribution** — OAC, `index.html` default root, 403/404 → `index.html` (200) for SPA routing, HTTP2, IPv6
  - **Cognito User Pool** — `reporte-ciudadano-admin-pool`, self sign-up disabled, strict password policy, app client `reporte-ciudadano-admin-web` (no secret, ALLOW_USER_PASSWORD_AUTH + ALLOW_REFRESH_TOKEN_AUTH, callback/logout URLs from `appDomain` context)
  - **GitHub OIDC IAM role** — `reporte-ciudadano-admin-deploy-role` trusts `token.actions.githubusercontent.com`; grants ECR push, ECS rolling deploy, S3 sync, CloudFront invalidation
  - **CfnOutputs** — ALB DNS, CloudFront domain, Cognito pool ID, Cognito client ID, ECR URI, GitHub deploy role ARN, cluster/service/bucket names
- `infra/package.json` — CDK v2 (2.180.0) + TypeScript dependencies
- `infra/tsconfig.json` — strict TypeScript compiler config targeting ES2020
- `infra/cdk.json` — CDK app pointer + all CDK v2 feature flags; `acmCertArn` and `appDomain` context keys for customisation
- `infra/.gitignore` — excludes `node_modules`, `dist`, `cdk.out`

---

## [0.5.0] — 2026-06-01

### Added — FEAT-004 Cognito JWT Authentication

- `plugins/Auth.kt` — `configureAuth()` installs Ktor `Authentication` plugin with a `jwt("cognito")` provider; JWKS fetched from `https://cognito-idp.us-east-1.amazonaws.com/${COGNITO_USER_POOL_ID}/.well-known/jwks.json`; keys cached (10 / 24 h) and rate-limited (10 req/min); `validate` block checks `client_id` claim equals `COGNITO_CLIENT_ID` env var
- `plugins/Routing.kt` — `configureRouting()` wraps all `/api` routes inside `authenticate("cognito") { }`, keeping `GET /health` public
- `Application.kt` — inline routing removed; `configureAuth()` called before `configureRouting()`

---

## [0.4.0] — 2026-06-01

### Added — FEAT-009 CI/CD Pipelines

- `.github/workflows/ci.yml` — CI checks on every push/PR: backend compile, unit tests, ktlint, Docker build, Trivy vulnerability scan (SARIF → GitHub Security tab), frontend WASM compile + production webpack
- `.github/workflows/cd-backend.yml` — on push to `main`: build fat JAR, push to ECR, rolling ECS deploy, wait for stability (OIDC auth, no long-lived AWS keys)
- `.github/workflows/cd-frontend.yml` — on push to `main`: production WASM build, S3 sync with correct cache headers, CloudFront invalidation
- `.github/pull_request_template.md` — PR checklist: tests, lint, Docker build, features.md/changelog.md updates
- `.github/dependabot.yml` — weekly auto-updates for Gradle, GitHub Actions, and Docker dependencies
- `.editorconfig` — ktlint `intellij_idea` code style, 120 char line length, no wildcard imports
- `gradle/libs.versions.toml` — added `ktlint-gradle 12.2.0` plugin alias
- `backend/build.gradle.kts` — applied `org.jlleitschuh.gradle.ktlint` plugin

---

## [0.3.0] — 2026-06-01

### Added — FEAT-003 Backend API (DynamoDB + S3 read)

- `domain/ReportRepository.kt` + `domain/PhotoRepository.kt` — repository interfaces decoupling routes from AWS
- `aws/DynamoDbClient.kt` — `DynamoReportRepository`: `Scan` with optional status filter + `UpdateItem` for status changes
- `aws/S3Client.kt` — `S3PhotoRepository`: `ListObjectsV2` for photo keys + `presignGetObject` (15-min TTL)
- `routes/ReportsRoutes.kt` — `GET /api/reports`, `GET /api/reports/{id}`, `PUT /api/reports/{id}/status`
- `routes/PhotosRoutes.kt` — `GET /api/reports/{id}/photos`, `GET /api/reports/{id}/photos/{name}/url`
- `dto/Dto.kt` — serializable request/response DTOs
- `backend/src/test/` — 9 unit tests (`ReportsRoutesTest`, `PhotosRoutesTest`) using `ktor-server-test-host` with fake repositories
- `gradle/libs.versions.toml` — added `ktor-server-test-host` alias
- `backend/build.gradle.kts` — added `testImplementation` dependencies

---

## [0.2.0] — 2026-06-01

### Changed — FEAT-002 Gradle 9 Upgrade

- Gradle wrapper bumped from 8.10.2 to 9.5.1
- Shadow plugin migrated from `com.github.johnrengelman.shadow` to `com.gradleup.shadow` 9.0.0 (Gradle 9 compatible fork)
- Kotlin bumped from 2.1.0 to 2.3.0
- Compose Multiplatform bumped from 1.7.3 to 1.8.0
- kotlinx-serialization bumped from 1.7.3 to 1.8.0
- Frontend dev server pinned to port 3000 (avoids conflict with backend on 8080)
- `gradle.properties` added with `-Xmx4g` for Gradle and Kotlin daemon

---

## [0.1.0] — 2026-06-01

### Added — FEAT-001 Project Scaffolding

- Gradle multi-project setup (`:backend`, `:frontend`) with version catalog (`gradle/libs.versions.toml`)
- `:backend` — Ktor 3.1.3 (Netty) JVM module; Shadow plugin fat JAR; `GET /health` route; domain models `Report` + `ReportStatus`
- `:frontend` — Compose Multiplatform 1.7.3 wasmJs module; placeholder `ComposeViewport` entry point; `index.html` with Leaflet 1.9.4 pre-loaded
- `backend/Dockerfile` — multi-stage build on eclipse-temurin:21; minimal JRE runtime image
- `docker-compose.yml` — local dev stack with `~/.aws` credentials mount
- `.gitignore` — excludes build artefacts and credentials
- `CLAUDE.md` — self-contained project context for AI sessions
