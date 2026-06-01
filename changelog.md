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

## [0.7.0] — 2026-06-01

### Added — FEAT-005/006/007 Compose WASM Frontend

#### FEAT-007 Reports Map View
- `ui/screens/ReportsMapScreen.kt` — `DisposableEffect`-based Leaflet 1.9.4 map; fetches up to 500 reports and places a marker per report at its lat/lng; marker popup shows title + status label; clicking a marker calls `onReportSelected(id)`; map `<div>` is hidden (not destroyed) when navigating away
- Leaflet JS interop via top-level `js()` functions (`leafletMap`, `leafletMarker`, `leafletTileLayer`, `leafletLatLng`, `fitBounds`, `bindPopup`, `onMarkerClick`, etc.) — `dynamic` is not available in wasmJs

#### FEAT-006 Report Detail + Status Update
- `ui/screens/ReportDetailScreen.kt` — displays title, description, lat/lng, creation date, current `StatusBadge`; `ExposedDropdownMenuBox` lists all `ReportStatus` values; Save button calls `ApiClient.updateStatus()` inside `rememberCoroutineScope`; success/error communicated via Material3 `Snackbar`; photo thumbnails rendered as HTML `<img>` elements via `DisposableEffect` using presigned S3 URLs from `ApiClient.getPhotoUrl()`

#### FEAT-005 Reports List Screen
- `ui/screens/ReportsListScreen.kt` — `LazyRow` of `FilterChip` status filters + `LazyColumn` of `ReportCard` items; pagination via `nextKey` / "Load more" button; loading spinner and error-with-retry states
- `ui/components/StatusBadge.kt` — coloured rounded `Box` + `Text` for each `ReportStatus` value
- `ui/components/ReportCard.kt` — clickable `Card` with title, `StatusBadge`, and formatted creation date

#### Shared frontend additions
- `domain/ReportStatus.kt` — frontend enum mirroring backend: `SENT, SEEN, PENDING, IN_PROGRESS, RESOLVED, DISCARDED`; `label` property with user-friendly strings
- `domain/Report.kt` — serializable data class matching backend `Report`; `createdAtFormatted` computed via `js("new Date(...).toLocaleDateString(...)")`
- `network/ApiClient.kt` — Ktor JS `HttpClient` with `ContentNegotiation`; base URL from `window.location.origin`; methods: `getReports`, `getReport`, `updateStatus`, `getPhotoKeys`, `getPhotoUrl`; every request attaches `Authorization: Bearer <token>` from `AuthStore`
- `network/AuthStore.kt` — `sessionStorage`-backed token store (`save`, `load`, `clear`)
- `network/Dto.kt` — `ReportsResponse`, `PhotoKeysResponse`, `PhotoUrlResponse`, `StatusUpdateRequest`
- `ui/screens/LoginScreen.kt` — "Sign in with Cognito" button; redirects to Cognito Hosted UI using `COGNITO_DOMAIN` + `COGNITO_CLIENT_ID` read from `window.__ENV__`
- `main.kt` — state-based router (`Screen.Login / List / Detail / Map`); `LaunchedEffect` handles `?code=` OAuth callback, exchanges code via Ktor `submitForm`, stores token, clears URL; bottom `NavigationBar` tabs for List / Map
- `resources/index.html` — added `<script>window.__ENV__ = { COGNITO_DOMAIN: "", COGNITO_CLIENT_ID: "" };</script>` placeholder for runtime injection by CloudFront / CDK
- `gradle/libs.versions.toml` — added `kotlinx-coroutines 1.10.2` version + library alias
- `frontend/build.gradle.kts` — added `libs.kotlinx.coroutines.core` dependency

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
