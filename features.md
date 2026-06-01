# Features — ReporteCiudadanoAdmin

Feature tracking for the admin dashboard. Each entry follows the pipeline:
`Draft → Design → Ready → In Progress → Done`

---

## Feature Template

```
### FEAT-NNN — Feature Name
**Status**: Draft | Design | Ready | In Progress | Done
**Branch**: feature/feat-NNN-short-name

#### Architect Notes
(filled by architect agent)

#### User Story
As a [role], I want [action] so that [benefit].

#### Acceptance Criteria
- [ ] Given ... when ... then ...

#### UX/UI Proposal
(filled by designer agent)
```

---

### FEAT-009 — CI/CD Pipelines
**Status**: Done
**Branch**: `feature/feat-009-cicd`

#### Architect Notes
GitHub Actions CI workflow runs on every push and PR: backend compile, unit tests, ktlint lint, Docker build, and Trivy vulnerability scan (CRITICAL/HIGH, ignores unfixed). Frontend WASM compile + production webpack. CD workflows (cd-backend.yml, cd-frontend.yml) deploy to ECS and S3/CloudFront on push to main using OIDC (no long-lived AWS keys in GitHub). Gradle cache via `gradle/actions/setup-gradle`. PR template and Dependabot (Gradle + Actions + Docker) also included.

#### User Story
As a developer, I want automated CI checks on every PR so that broken builds, failing tests, lint violations, and known vulnerabilities are caught before merging.

#### Acceptance Criteria
- [x] `ci.yml` triggers on push to any branch and PRs targeting main
- [x] Backend unit tests run and report is uploaded on failure
- [x] ktlint check enforced (`ktlintCheck` task via `jlleitschuh/ktlint-gradle` 12.2.0)
- [x] Docker image builds and Trivy scans for CRITICAL/HIGH CVEs; SARIF uploaded to GitHub Security tab
- [x] Frontend WASM compiles and production webpack succeeds
- [x] `cd-backend.yml` builds fat JAR, pushes to ECR, rolling-deploys to ECS via OIDC
- [x] `cd-frontend.yml` syncs WASM assets to S3 with correct cache headers and invalidates CloudFront
- [x] Dependabot configured for Gradle, GitHub Actions, and Docker weekly updates
- [x] PR template guides contributors to run tests, lint, and update changelog

---

### FEAT-008 — AWS CDK Infrastructure Stack
**Status**: Done
**Branch**: `feature/feat-008-cdk-infra`

#### Architect Notes
AWS CDK v2 (TypeScript) stack at `infra/` provisions all cloud infrastructure: ECR repository (`reporte-ciudadano-admin-backend`), ECS Fargate cluster (`reporte-ciudadano-admin`) on a 2-AZ VPC with 1 NAT gateway, Fargate task definition (512 CPU / 1024 MB, port 8080), task IAM role scoped to DynamoDB Scan/GetItem/UpdateItem/DescribeTable and S3 GetObject/HeadObject/ListBucket, Cognito env vars sourced from SSM Parameter Store, internet-facing ALB with HTTP→HTTPS redirect and HTTPS listener using ACM cert from context, private S3 bucket (`reporte-ciudadano-admin-frontend`) with versioning enabled, CloudFront distribution with OAC, SPA error responses (403/404 → index.html 200), Cognito User Pool (`reporte-ciudadano-admin-pool`) with self sign-up disabled and strict password policy, and a GitHub Actions OIDC IAM role (`reporte-ciudadano-admin-deploy-role`) for zero-credential CI/CD. All key ARNs/names exported as CfnOutputs.

#### User Story
As a DevSecOps engineer, I want all AWS infrastructure declared as code in a CDK stack so that the environment can be created, reviewed, and destroyed reproducibly with no manual console work.

#### Acceptance Criteria
- [x] ECR repository `reporte-ciudadano-admin-backend` created with lifecycle rule (keep last 10 images)
- [x] ECS Fargate cluster on a 2-AZ VPC with 1 NAT gateway and private subnets for tasks
- [x] Task definition: 512 CPU / 1024 MB, port 8080, health-check `/health`, image from ECR
- [x] Task role `reporte-ciudadano-admin-task-role` grants DynamoDB (Scan, GetItem, UpdateItem, DescribeTable) and S3 read on photos bucket
- [x] `COGNITO_USER_POOL_ID` and `COGNITO_CLIENT_ID` injected into task via SSM Parameter Store secrets
- [x] Internet-facing ALB: HTTP (80) → HTTPS (443) redirect, HTTPS listener with ACM cert from `acmCertArn` context, health check path `/health`
- [x] Frontend S3 bucket `reporte-ciudadano-admin-frontend`: private, block all public access, versioning on
- [x] CloudFront distribution with OAC, `index.html` default root, 403→index.html and 404→index.html (SPA routing)
- [x] Cognito User Pool: self sign-up disabled, password ≥ 8 chars upper+lower+digit+symbol, app client with ALLOW_USER_PASSWORD_AUTH + ALLOW_REFRESH_TOKEN_AUTH, no client secret, callback/logout URLs from `appDomain` context
- [x] GitHub OIDC role `reporte-ciudadano-admin-deploy-role`: ECR push, ECS update-service/describe-services, S3 sync on frontend bucket, CloudFront create-invalidation
- [x] CfnOutputs exported: ALB DNS, CloudFront domain, Cognito pool ID, Cognito client ID, ECR URI, GitHub deploy role ARN

---

### FEAT-004 — Cognito JWT Authentication
**Status**: Done
**Branch**: `feature/feat-004-cognito-jwt-auth`

#### Architect Notes
Install `ktor-server-auth-jwt` plugin (already in dependencies) to validate Cognito access tokens on all `/api` routes. JWKS URL is fetched from the Cognito User Pool's well-known endpoint using `JwkProviderBuilder` (cached 10 keys / 24 h, rate-limited). The `validate` block checks `client_id` claim matches `COGNITO_CLIENT_ID` env var. `/health` remains unauthenticated. Auth configuration extracted to `plugins/Auth.kt` and routing extracted to `plugins/Routing.kt` so `Application.kt` stays thin.

#### User Story
As a government official, I want the admin dashboard API to reject requests without a valid Cognito access token so that report data is only accessible to authenticated users.

#### Acceptance Criteria
- [x] All `GET`/`PUT` routes under `/api` return `401` when `Authorization` header is absent
- [x] All `GET`/`PUT` routes under `/api` return `401` when the token is expired or has wrong `client_id`
- [x] All `GET`/`PUT` routes under `/api` return the expected response when a valid Cognito access token is supplied
- [x] `GET /health` returns `200` without any `Authorization` header
- [x] No long-lived credentials committed — pool ID and client ID supplied via `COGNITO_USER_POOL_ID` and `COGNITO_CLIENT_ID` env vars
- [x] JWKS keys are cached (10 keys, 24 h) and rate-limited to avoid hammering the Cognito endpoint

---

### FEAT-003 — Backend API (DynamoDB + S3 read)
**Status**: Done
**Branch**: `feature/feat-003-backend-api`

#### Architect Notes
Wire up all REST API routes backed by DynamoDB and S3. Auth (Cognito JWT) is deferred to FEAT-004. Top-level functions in `aws/DynamoDbClient.kt` and `aws/S3Client.kt` wrap the AWS SDK Kotlin clients; route extension functions in `routes/ReportsRoutes.kt` and `routes/PhotosRoutes.kt` keep the Ktor layer thin. Serialization DTOs live in `dto/Dto.kt`.

#### User Story
As a government official, I want the admin dashboard backend to serve report data from DynamoDB and photo URLs from S3 so that the frontend can display and update citizen reports.

#### Acceptance Criteria
- [x] `GET /api/reports` returns paginated report list from DynamoDB
- [x] `GET /api/reports/{id}` returns a single report or 404
- [x] `PUT /api/reports/{id}/status` updates report status in DynamoDB; returns 404 when ID absent
- [x] `GET /api/reports/{id}/photos` returns S3 object keys for the report
- [x] `GET /api/reports/{id}/photos/{name}/url` returns a 15-minute presigned S3 GET URL
- [x] `GET /health` still returns 200 without auth

---

### FEAT-002 — Gradle 9 Upgrade
**Status**: Done
**Branch**: `feature/feat-002-gradle-9-upgrade`

#### Architect Notes
Bump Gradle wrapper from 8.10.2 to 9.5.1. Switch Shadow plugin from `com.github.johnrengelman.shadow` (dropped Gradle 9 support) to `com.gradleup.shadow` at 9.0.0. Bump Kotlin to 2.3.0, Compose Multiplatform to 1.8.0, and kotlinx-serialization to 1.8.0. Frontend dev server pinned to port 3000 via `devServerProperty` in `runTask`.

#### User Story
As a developer, I want the build toolchain on Gradle 9 so that the project stays on a supported version and benefits from improved build performance and APIs.

#### Acceptance Criteria
- [x] `./gradlew :backend:compileKotlin` succeeds on Gradle 9.5.1
- [x] `./gradlew :backend:shadowJar` produces a runnable fat JAR
- [x] `./gradlew :frontend:wasmJsBrowserDevelopmentRun` starts the dev server
- [x] `docker compose up` starts and `GET /health` returns `200 ok`

---

### FEAT-001 — Project Scaffolding & Gradle Setup
**Status**: Done
**Branch**: `feature/feat-001-scaffold`

#### Architect Notes
Multi-project Gradle setup with `:backend` (Ktor JVM, Netty) and `:frontend` (Compose Multiplatform wasmJs). Version catalog in `gradle/libs.versions.toml`. Backend fat JAR via Shadow plugin. Gradle wrapper bootstrapped from mobile project. Domain models (`Report`, `ReportStatus`) declared. Dockerfile + docker-compose for local dev.

#### User Story
As a developer, I want a working Gradle project scaffold so that I can build and run both the backend and frontend from a single repository.

#### Acceptance Criteria
- [x] `./gradlew :backend:compileKotlin` succeeds with no errors
- [x] `./gradlew :backend:shadowJar` produces a runnable fat JAR
- [x] `backend/Dockerfile` builds a runnable image (multi-stage, eclipse-temurin:21)
- [x] `docker-compose up` starts the backend on port 8080
- [x] `GET /health` returns `200 ok`
- [x] `.gitignore` excludes credentials and build artefacts
- [x] Frontend `wasmJsMain` entry point compiles without errors
