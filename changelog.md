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
