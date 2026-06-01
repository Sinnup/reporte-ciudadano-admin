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
